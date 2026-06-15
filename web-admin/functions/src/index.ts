import { createHash } from "node:crypto";

import { getApps, initializeApp } from "firebase-admin/app";
import { getAuth } from "firebase-admin/auth";
import {
  FieldValue,
  Timestamp,
  getFirestore
} from "firebase-admin/firestore";
import { HttpsError, onCall } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import { onSchedule } from "firebase-functions/v2/scheduler";
import { google } from "googleapis";

if (!getApps().length) initializeApp();

const db = getFirestore();
const auth = getAuth();
const region = "asia-northeast1";
const playServiceAccount = defineSecret("GOOGLE_PLAY_SERVICE_ACCOUNT_JSON");
const androidPackageName = defineSecret("ANDROID_PACKAGE_NAME");

type AdminRole = "owner" | "admin" | "viewer";

const featureLabels: Record<string, string> = {
  steps: "歩数計",
  notes: "メモ",
  schedule: "予定",
  medicine: "薬アラーム",
  todos: "今日やること",
  shopping: "買い物リスト",
  family: "家族連絡先"
};

async function requireRole(
  uid: string | undefined,
  allowed: AdminRole[]
): Promise<AdminRole> {
  if (!uid) throw new HttpsError("unauthenticated", "ログインが必要です。");
  const snapshot = await db.collection("admins").doc(uid).get();
  const data = snapshot.data();
  if (!snapshot.exists || !data?.active || !allowed.includes(data.role)) {
    throw new HttpsError("permission-denied", "この操作を行う権限がありません。");
  }
  return data.role;
}

function text(value: unknown, name: string, maxLength = 240) {
  if (typeof value !== "string" || !value.trim() || value.length > maxLength) {
    throw new HttpsError("invalid-argument", `${name}が正しくありません。`);
  }
  return value.trim();
}

function csvCell(value: unknown) {
  return `"${String(value ?? "").replaceAll('"', '""')}"`;
}

export const setUserDisabled = onCall({ region }, async (request) => {
  await requireRole(request.auth?.uid, ["owner", "admin"]);
  const userId = text(request.data?.userId, "ユーザーID", 128);
  const disabled = request.data?.disabled;
  if (typeof disabled !== "boolean") {
    throw new HttpsError("invalid-argument", "停止状態が正しくありません。");
  }

  await Promise.all([
    auth.updateUser(userId, { disabled }),
    db.collection("users").doc(userId).set(
      {
        disabled,
        updatedAt: FieldValue.serverTimestamp(),
        updatedBy: request.auth?.uid
      },
      { merge: true }
    )
  ]);
  return { ok: true };
});

export const updateAppSettings = onCall({ region }, async (request) => {
  await requireRole(request.auth?.uid, ["owner", "admin"]);
  const data = request.data || {};
  if (
    typeof data.maintenanceMode !== "boolean" ||
    typeof data.forceUpdate !== "boolean"
  ) {
    throw new HttpsError("invalid-argument", "設定値が正しくありません。");
  }

  const minimumAppVersion = text(
    data.minimumAppVersion,
    "最低対応バージョン",
    30
  );
  const announcementMessage =
    typeof data.announcementMessage === "string"
      ? data.announcementMessage.trim().slice(0, 240)
      : "";

  await db.collection("app_settings").doc("global").set(
    {
      maintenanceMode: data.maintenanceMode,
      minimumAppVersion,
      forceUpdate: data.forceUpdate,
      announcementMessage,
      updatedAt: FieldValue.serverTimestamp()
    },
    { merge: true }
  );
  return { ok: true };
});

export const upsertAdminRole = onCall({ region }, async (request) => {
  await requireRole(request.auth?.uid, ["owner"]);
  const identifier = text(request.data?.identifier, "メールまたはUID", 320);
  const role = request.data?.role as AdminRole;
  if (!["owner", "admin", "viewer"].includes(role)) {
    throw new HttpsError("invalid-argument", "役割が正しくありません。");
  }

  const user = identifier.includes("@")
    ? await auth.getUserByEmail(identifier)
    : await auth.getUser(identifier);

  const adminRef = db.collection("admins").doc(user.uid);
  const existing = await adminRef.get();
  await adminRef.set(
    {
      email: user.email || "",
      displayName: user.displayName || user.email || "管理者",
      role,
      active: true,
      ...(existing.exists ? {} : { createdAt: FieldValue.serverTimestamp() }),
      updatedAt: FieldValue.serverTimestamp(),
      updatedBy: request.auth?.uid
    },
    { merge: true }
  );
  return { ok: true, uid: user.uid };
});

export const removeAdmin = onCall({ region }, async (request) => {
  await requireRole(request.auth?.uid, ["owner"]);
  const uid = text(request.data?.uid, "管理者UID", 128);
  if (uid === request.auth?.uid) {
    throw new HttpsError("failed-precondition", "自分の権限は削除できません。");
  }

  const target = await db.collection("admins").doc(uid).get();
  if (target.data()?.role === "owner") {
    const ownerCount = await db
      .collection("admins")
      .where("role", "==", "owner")
      .where("active", "==", true)
      .count()
      .get();
    if (ownerCount.data().count <= 1) {
      throw new HttpsError(
        "failed-precondition",
        "最後のオーナーは削除できません。"
      );
    }
  }
  await target.ref.delete();
  return { ok: true };
});

export const exportUsersCsv = onCall({ region }, async (request) => {
  await requireRole(request.auth?.uid, ["owner", "admin"]);
  const snapshot = await db
    .collection("users")
    .orderBy("createdAt", "desc")
    .limit(10000)
    .get();

  const header = [
    "UID",
    "名前",
    "メール",
    "プラン",
    "プレミアム",
    "停止中",
    "登録日",
    "最終利用",
    "アプリ版"
  ];
  const rows = snapshot.docs.map((doc) => {
    const data = doc.data();
    return [
      doc.id,
      data.displayName,
      data.email,
      data.plan,
      data.premium,
      data.disabled,
      data.createdAt?.toDate?.().toISOString() || "",
      data.lastActiveAt?.toDate?.().toISOString() || "",
      data.appVersion
    ];
  });
  const csv = [header, ...rows]
    .map((row) => row.map(csvCell).join(","))
    .join("\n");
  return { csv };
});

export const verifyGooglePlayPurchase = onCall(
  {
    region,
    secrets: [playServiceAccount, androidPackageName]
  },
  async (request) => {
    const uid = request.auth?.uid;
    if (!uid) throw new HttpsError("unauthenticated", "ログインが必要です。");

    const purchaseToken = text(
      request.data?.purchaseToken,
      "購入トークン",
      4096
    );
    const productId = text(request.data?.productId, "商品ID", 120);
    const purchaseType = request.data?.purchaseType;
    if (!["subscription", "product"].includes(purchaseType)) {
      throw new HttpsError("invalid-argument", "購入種類が正しくありません。");
    }
    if (
      (purchaseType === "subscription" && productId !== "premium_monthly") ||
      (purchaseType === "product" && productId !== "premium_lifetime")
    ) {
      throw new HttpsError("invalid-argument", "許可されていない商品です。");
    }

    const credentials = JSON.parse(playServiceAccount.value());
    const googleAuth = new google.auth.GoogleAuth({
      credentials,
      scopes: ["https://www.googleapis.com/auth/androidpublisher"]
    });
    const publisher = google.androidpublisher({
      version: "v3",
      auth: googleAuth
    });
    const packageName = androidPackageName.value();

    let status: "active" | "cancelled" | "expired" = "expired";
    let premium = false;
    let expiresAt: Timestamp | null = null;
    let startedAt = Timestamp.now();

    if (purchaseType === "subscription") {
      const response = await publisher.purchases.subscriptionsv2.get({
        packageName,
        token: purchaseToken
      });
      const state = response.data.subscriptionState || "";
      const expiryValue = response.data.lineItems?.[0]?.expiryTime;
      const expiryDate = expiryValue ? new Date(expiryValue) : null;
      const hasTime = expiryDate && expiryDate.getTime() > Date.now();
      premium = Boolean(hasTime) && !state.includes("EXPIRED");
      status = state.includes("CANCELED")
        ? "cancelled"
        : premium
          ? "active"
          : "expired";
      expiresAt = expiryDate ? Timestamp.fromDate(expiryDate) : null;
      const startValue = response.data.startTime;
      startedAt = startValue
        ? Timestamp.fromDate(new Date(startValue))
        : Timestamp.now();
    } else {
      const response = await publisher.purchases.products.get({
        packageName,
        productId,
        token: purchaseToken
      });
      premium = response.data.purchaseState === 0;
      status = premium ? "active" : "expired";
      startedAt = response.data.purchaseTimeMillis
        ? Timestamp.fromMillis(Number(response.data.purchaseTimeMillis))
        : Timestamp.now();
    }

    const tokenHash = createHash("sha256").update(purchaseToken).digest("hex");
    const subscriptionRef = db.collection("subscriptions").doc(tokenHash);
    const userRecord = await auth.getUser(uid);

    await db.runTransaction(async (transaction) => {
      transaction.set(
        subscriptionRef,
        {
          userId: uid,
          userName: userRecord.displayName || userRecord.email || "名前未設定",
          status,
          plan: purchaseType === "subscription" ? "monthly" : "lifetime",
          productId,
          purchaseTokenHash: tokenHash,
          startedAt,
          expiresAt,
          source: "google_play",
          verifiedAt: FieldValue.serverTimestamp()
        },
        { merge: true }
      );
      transaction.set(
        db.collection("users").doc(uid),
        {
          premium,
          plan: premium
            ? purchaseType === "subscription"
              ? "monthly"
              : "lifetime"
            : "free",
          premiumVerifiedAt: FieldValue.serverTimestamp()
        },
        { merge: true }
      );
    });

    return {
      premium,
      status,
      plan: premium
        ? purchaseType === "subscription"
          ? "monthly"
          : "lifetime"
        : "free",
      expiresAt: expiresAt?.toDate().toISOString() || null
    };
  }
);

export const aggregateDailyUsage = onSchedule(
  {
    region,
    schedule: "10 1 * * *",
    timeZone: "Asia/Tokyo"
  },
  async () => {
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const yesterday = new Date(today);
    yesterday.setDate(yesterday.getDate() - 1);
    const dateKey = yesterday.toISOString().slice(0, 10);

    const eventsSnapshot = await db
      .collection("user_events")
      .where("occurredAt", ">=", Timestamp.fromDate(yesterday))
      .where("occurredAt", "<", Timestamp.fromDate(today))
      .get();

    const featureCounts: Record<
      string,
      { label: string; count: number; users: Set<string> }
    > = {};
    const activeUsers = new Set<string>();
    for (const event of eventsSnapshot.docs) {
      const data = event.data();
      const feature = String(data.feature || "unknown");
      const userId = String(data.userId || "");
      activeUsers.add(userId);
      featureCounts[feature] ||= {
        label: featureLabels[feature] || feature,
        count: 0,
        users: new Set<string>()
      };
      featureCounts[feature].count += 1;
      featureCounts[feature].users.add(userId);
    }

    const [usersCount, premiumCount, subscriptionCount] = await Promise.all([
      db.collection("users").count().get(),
      db.collection("users").where("premium", "==", true).count().get(),
      db
        .collection("subscriptions")
        .where("status", "==", "active")
        .count()
        .get()
    ]);

    const features = Object.fromEntries(
      Object.entries(featureCounts).map(([feature, value]) => [
        feature,
        {
          label: value.label,
          count: value.count,
          uniqueUsers: value.users.size
        }
      ])
    );

    await db.collection("feature_usage_daily").doc(dateKey).set({
      date: dateKey.slice(5).replace("-", "/"),
      dateValue: Timestamp.fromDate(yesterday),
      users: usersCount.data().count,
      activeUsers: activeUsers.size,
      premiumUsers: premiumCount.data().count,
      subscriptions: subscriptionCount.data().count,
      features,
      updatedAt: FieldValue.serverTimestamp()
    });

    const recentDaily = await db
      .collection("feature_usage_daily")
      .orderBy("dateValue", "desc")
      .limit(30)
      .get();
    const totals: Record<string, { label: string; total: number; uniqueUsers: number }> =
      {};
    for (const day of recentDaily.docs) {
      const dayFeatures = day.data().features || {};
      for (const [feature, value] of Object.entries(dayFeatures) as Array<
        [string, { label: string; count: number; uniqueUsers: number }]
      >) {
        totals[feature] ||= { label: value.label, total: 0, uniqueUsers: 0 };
        totals[feature].total += Number(value.count || 0);
        totals[feature].uniqueUsers = Math.max(
          totals[feature].uniqueUsers,
          Number(value.uniqueUsers || 0)
        );
      }
    }

    const batch = db.batch();
    for (const [feature, value] of Object.entries(totals)) {
      batch.set(
        db.collection("feature_usage").doc(feature),
        {
          feature,
          label: value.label,
          total: value.total,
          uniqueUsers: value.uniqueUsers,
          change: 0,
          updatedAt: FieldValue.serverTimestamp()
        },
        { merge: true }
      );
    }
    await batch.commit();
  }
);
