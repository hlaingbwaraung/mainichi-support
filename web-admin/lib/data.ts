import "server-only";

import { Timestamp } from "firebase-admin/firestore";

import { adminDb, isMockServer } from "@/lib/firebase/admin";
import {
  getMockUserDetail,
  mockAdmins,
  mockDashboard,
  mockFeatureDaily,
  mockFeatureUsage,
  mockSettings,
  mockSubscriptions,
  mockUsers
} from "@/lib/mock-data";
import type {
  AdminProfile,
  AppSettings,
  AppUser,
  DashboardData,
  FeatureDailyMetric,
  FeatureUsage,
  Subscription,
  UserDetailData,
  UserEvent
} from "@/lib/types";

function iso(value: unknown): string {
  if (value instanceof Timestamp) return value.toDate().toISOString();
  if (value instanceof Date) return value.toISOString();
  if (typeof value === "string") return value;
  return new Date().toISOString();
}

function userFromDoc(id: string, data: Record<string, unknown>): AppUser {
  return {
    id,
    displayName: String(data.displayName || "名前未設定"),
    email: data.email ? String(data.email) : undefined,
    phone: data.phone ? String(data.phone) : undefined,
    plan: (data.plan as AppUser["plan"]) || "free",
    premium: Boolean(data.premium),
    disabled: Boolean(data.disabled),
    appVersion: String(data.appVersion || "不明"),
    platform: "android",
    createdAt: iso(data.createdAt),
    lastActiveAt: iso(data.lastActiveAt),
    stepCountToday: Number(data.stepCountToday || 0),
    familyContactCount: Number(data.familyContactCount || 0)
  };
}

function subscriptionFromDoc(
  id: string,
  data: Record<string, unknown>
): Subscription {
  return {
    id,
    userId: String(data.userId),
    userName: String(data.userName || "名前未設定"),
    status: data.status as Subscription["status"],
    plan: data.plan as Subscription["plan"],
    productId: String(data.productId),
    startedAt: iso(data.startedAt),
    expiresAt: data.expiresAt ? iso(data.expiresAt) : undefined,
    cancelledAt: data.cancelledAt ? iso(data.cancelledAt) : undefined,
    source: "google_play"
  };
}

export async function getUsers(): Promise<AppUser[]> {
  if (isMockServer || !adminDb) return mockUsers;
  const snapshot = await adminDb
    .collection("users")
    .orderBy("createdAt", "desc")
    .limit(500)
    .get();
  return snapshot.docs.map((doc) => userFromDoc(doc.id, doc.data()));
}

export async function getSubscriptions(): Promise<Subscription[]> {
  if (isMockServer || !adminDb) return mockSubscriptions;
  const snapshot = await adminDb
    .collection("subscriptions")
    .orderBy("startedAt", "desc")
    .limit(500)
    .get();
  return snapshot.docs.map((doc) => subscriptionFromDoc(doc.id, doc.data()));
}

export async function getDashboardData(): Promise<DashboardData> {
  if (isMockServer || !adminDb) return mockDashboard;

  const [users, subscriptions, usageSnapshot, dailySnapshot] = await Promise.all([
    getUsers(),
    getSubscriptions(),
    adminDb.collection("feature_usage").orderBy("total", "desc").limit(10).get(),
    adminDb
      .collection("feature_usage_daily")
      .orderBy("dateValue", "desc")
      .limit(14)
      .get()
  ]);

  const sevenDaysAgo = Date.now() - 7 * 86400000;
  const featureUsage = usageSnapshot.docs.map((doc) => ({
    feature: doc.data().feature,
    label: doc.data().label,
    total: Number(doc.data().total || 0),
    uniqueUsers: Number(doc.data().uniqueUsers || 0),
    change: Number(doc.data().change || 0)
  })) as FeatureUsage[];

  const metrics = dailySnapshot.docs
    .map((doc) => ({
      date: String(doc.data().date),
      users: Number(doc.data().users || users.length),
      activeUsers: Number(doc.data().activeUsers || 0),
      premiumUsers: Number(
        doc.data().premiumUsers || users.filter((user) => user.premium).length
      ),
      subscriptions: Number(doc.data().subscriptions || 0)
    }))
    .reverse();

  return {
    totalUsers: users.length,
    activeUsers: users.filter(
      (user) => new Date(user.lastActiveAt).getTime() >= sevenDaysAgo
    ).length,
    premiumUsers: users.filter((user) => user.premium).length,
    freeUsers: users.filter((user) => !user.premium).length,
    activeSubscriptions: subscriptions.filter((sub) => sub.status === "active")
      .length,
    cancelledSubscriptions: subscriptions.filter(
      (sub) => sub.status === "cancelled"
    ).length,
    metrics,
    featureUsage,
    recentUsers: users.slice(0, 5)
  };
}

export async function getUserDetail(id: string): Promise<UserDetailData | null> {
  if (isMockServer || !adminDb) return getMockUserDetail(id);

  const [userSnapshot, subscriptionSnapshot, eventsSnapshot] = await Promise.all([
    adminDb.collection("users").doc(id).get(),
    adminDb
      .collection("subscriptions")
      .where("userId", "==", id)
      .orderBy("startedAt", "desc")
      .limit(1)
      .get(),
    adminDb
      .collection("user_events")
      .where("userId", "==", id)
      .orderBy("occurredAt", "desc")
      .limit(25)
      .get()
  ]);

  if (!userSnapshot.exists) return null;
  const recentEvents = eventsSnapshot.docs.map((doc) => ({
    id: doc.id,
    userId: id,
    feature: doc.data().feature,
    eventName: doc.data().eventName,
    occurredAt: iso(doc.data().occurredAt),
    appVersion: doc.data().appVersion || "不明"
  })) as UserEvent[];

  return {
    user: userFromDoc(userSnapshot.id, userSnapshot.data() || {}),
    subscription: subscriptionSnapshot.empty
      ? undefined
      : subscriptionFromDoc(
          subscriptionSnapshot.docs[0].id,
          subscriptionSnapshot.docs[0].data()
        ),
    recentEvents
  };
}

export async function getFeatureAnalytics(): Promise<{
  summary: FeatureUsage[];
  daily: FeatureDailyMetric[];
}> {
  if (isMockServer || !adminDb) {
    return { summary: mockFeatureUsage, daily: mockFeatureDaily };
  }

  const [summarySnapshot, dailySnapshot] = await Promise.all([
    adminDb.collection("feature_usage").orderBy("total", "desc").get(),
    adminDb
      .collection("feature_usage_daily")
      .orderBy("dateValue", "desc")
      .limit(30)
      .get()
  ]);

  return {
    summary: summarySnapshot.docs.map((doc) => doc.data() as FeatureUsage),
    daily: dailySnapshot.docs
      .flatMap((doc) => {
        const data = doc.data();
        const features = (data.features || {}) as Record<
          string,
          { label: string; count: number }
        >;
        return Object.entries(features).map(([feature, value]) => ({
          date: String(data.date),
          feature,
          label: value.label,
          count: Number(value.count || 0)
        })) as FeatureDailyMetric[];
      })
      .reverse()
  };
}

export async function getAppSettings(): Promise<AppSettings> {
  if (isMockServer || !adminDb) return mockSettings;
  const snapshot = await adminDb.collection("app_settings").doc("global").get();
  if (!snapshot.exists) return mockSettings;
  const data = snapshot.data() || {};
  return {
    id: "global",
    maintenanceMode: Boolean(data.maintenanceMode),
    minimumAppVersion: String(data.minimumAppVersion || "1.0.0"),
    forceUpdate: Boolean(data.forceUpdate),
    announcementMessage: String(data.announcementMessage || ""),
    updatedAt: iso(data.updatedAt)
  };
}

export async function getAdmins(): Promise<AdminProfile[]> {
  if (isMockServer || !adminDb) return mockAdmins;
  const snapshot = await adminDb.collection("admins").orderBy("createdAt").get();
  return snapshot.docs.map((doc) => {
    const data = doc.data();
    return {
      id: doc.id,
      email: data.email,
      displayName: data.displayName || "管理者",
      role: data.role,
      active: data.active,
      createdAt: iso(data.createdAt),
      lastLoginAt: data.lastLoginAt ? iso(data.lastLoginAt) : undefined
    };
  });
}
