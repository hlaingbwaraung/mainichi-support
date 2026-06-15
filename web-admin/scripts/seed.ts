import { applicationDefault, cert, getApps, initializeApp } from "firebase-admin/app";
import { getFirestore, Timestamp } from "firebase-admin/firestore";

import {
  mockAdmins,
  mockEvents,
  mockFeatureDaily,
  mockFeatureUsage,
  mockSettings,
  mockSubscriptions,
  mockUsers
} from "../lib/mock-data";

function initialize() {
  if (getApps().length) return getApps()[0];
  const privateKey = process.env.FIREBASE_PRIVATE_KEY?.replace(/\\n/g, "\n");
  if (
    process.env.FIREBASE_PROJECT_ID &&
    process.env.FIREBASE_CLIENT_EMAIL &&
    privateKey
  ) {
    return initializeApp({
      credential: cert({
        projectId: process.env.FIREBASE_PROJECT_ID,
        clientEmail: process.env.FIREBASE_CLIENT_EMAIL,
        privateKey
      })
    });
  }
  return initializeApp({ credential: applicationDefault() });
}

function timestamp(value?: string) {
  return value ? Timestamp.fromDate(new Date(value)) : null;
}

async function seed() {
  const db = getFirestore(initialize());
  const ownerUid = process.env.SEED_OWNER_UID;
  const batch = db.batch();

  for (const user of mockUsers) {
    batch.set(db.collection("users").doc(user.id), {
      ...user,
      createdAt: timestamp(user.createdAt),
      lastActiveAt: timestamp(user.lastActiveAt)
    });
  }

  for (const subscription of mockSubscriptions) {
    batch.set(db.collection("subscriptions").doc(subscription.id), {
      ...subscription,
      startedAt: timestamp(subscription.startedAt),
      expiresAt: timestamp(subscription.expiresAt),
      cancelledAt: timestamp(subscription.cancelledAt)
    });
  }

  for (const event of mockEvents) {
    batch.set(db.collection("user_events").doc(event.id), {
      ...event,
      occurredAt: timestamp(event.occurredAt)
    });
  }

  for (const usage of mockFeatureUsage) {
    batch.set(db.collection("feature_usage").doc(usage.feature), usage);
  }

  const dailyByDate = new Map<string, typeof mockFeatureDaily>();
  for (const item of mockFeatureDaily) {
    const current = dailyByDate.get(item.date) || [];
    current.push(item);
    dailyByDate.set(item.date, current);
  }
  let dateOffset = 6;
  for (const [date, items] of dailyByDate) {
    const dateValue = new Date();
    dateValue.setHours(0, 0, 0, 0);
    dateValue.setDate(dateValue.getDate() - dateOffset);
    dateOffset -= 1;
    batch.set(db.collection("feature_usage_daily").doc(date.replace("/", "-")), {
      date,
      dateValue: Timestamp.fromDate(dateValue),
      users: mockUsers.length,
      activeUsers: Math.max(5, mockUsers.length - dateOffset),
      premiumUsers: mockUsers.filter((user) => user.premium).length,
      subscriptions: mockSubscriptions.filter((item) => item.status === "active")
        .length,
      features: Object.fromEntries(
        items.map((item) => [
          item.feature,
          {
            label: item.label,
            count: item.count,
            uniqueUsers: Math.min(mockUsers.length, Math.ceil(item.count / 8))
          }
        ])
      )
    });
  }

  batch.set(db.collection("app_settings").doc("global"), {
    ...mockSettings,
    updatedAt: timestamp(mockSettings.updatedAt)
  });

  for (const admin of mockAdmins) {
    const uid = admin.role === "owner" && ownerUid ? ownerUid : admin.id;
    batch.set(db.collection("admins").doc(uid), {
      ...admin,
      id: undefined,
      createdAt: timestamp(admin.createdAt),
      lastLoginAt: timestamp(admin.lastLoginAt)
    });
  }

  await batch.commit();
  console.log(`Seeded ${mockUsers.length} users and dashboard sample data.`);
  if (!ownerUid) {
    console.log(
      "Set SEED_OWNER_UID to your Firebase Auth UID before using real admin login."
    );
  }
}

seed().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
