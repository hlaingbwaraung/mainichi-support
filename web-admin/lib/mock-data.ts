import type {
  AdminProfile,
  AppSettings,
  AppUser,
  DashboardData,
  FeatureDailyMetric,
  FeatureKey,
  FeatureUsage,
  Subscription,
  UserDetailData,
  UserEvent
} from "@/lib/types";

const now = new Date("2026-06-16T09:30:00+09:00");

function daysAgo(days: number, hour = 9) {
  const date = new Date(now);
  date.setDate(date.getDate() - days);
  date.setHours(hour, 0, 0, 0);
  return date.toISOString();
}

const names = [
  "田中 春子",
  "佐藤 正一",
  "鈴木 和子",
  "高橋 健",
  "伊藤 洋子",
  "渡辺 博",
  "山本 幸子",
  "中村 明",
  "小林 恵子",
  "加藤 勇",
  "吉田 久美子",
  "山田 誠"
];

export const mockUsers: AppUser[] = names.map((displayName, index) => {
  const premium = index < 5;
  return {
    id: `user-${String(index + 1).padStart(3, "0")}`,
    displayName,
    email: `user${index + 1}@example.jp`,
    phone: `090-0000-${String(1000 + index)}`,
    plan: premium ? (index === 4 ? "lifetime" : "monthly") : "free",
    premium,
    disabled: index === 10,
    appVersion: index < 9 ? "2.2.0" : "2.1.0",
    platform: "android",
    createdAt: daysAgo(95 - index * 7),
    lastActiveAt: daysAgo(index % 8, 8 + (index % 6)),
    stepCountToday: 1800 + index * 487,
    familyContactCount: premium ? Math.min(5, index + 1) : 1
  };
});

export const mockSubscriptions: Subscription[] = mockUsers
  .filter((user) => user.premium)
  .map((user, index) => ({
    id: `sub-${index + 1}`,
    userId: user.id,
    userName: user.displayName,
    status: index === 3 ? "cancelled" : "active",
    plan: user.plan === "lifetime" ? "lifetime" : "monthly",
    productId:
      user.plan === "lifetime"
        ? "mainichi_premium_lifetime"
        : "mainichi_premium_monthly",
    startedAt: daysAgo(72 - index * 9),
    expiresAt:
      user.plan === "monthly" ? daysAgo(-(18 + index * 6)) : undefined,
    cancelledAt: index === 3 ? daysAgo(4) : undefined,
    source: "google_play"
  }));

export const mockAdmins: AdminProfile[] = [
  {
    id: "owner-001",
    email: "owner@mainichi.app",
    displayName: "オーナー",
    role: "owner",
    active: true,
    createdAt: daysAgo(120),
    lastLoginAt: daysAgo(0)
  },
  {
    id: "admin-001",
    email: "support@mainichi.app",
    displayName: "サポート担当",
    role: "admin",
    active: true,
    createdAt: daysAgo(40),
    lastLoginAt: daysAgo(2)
  },
  {
    id: "viewer-001",
    email: "analyst@mainichi.app",
    displayName: "分析担当",
    role: "viewer",
    active: true,
    createdAt: daysAgo(25),
    lastLoginAt: daysAgo(1)
  }
];

export const mockSettings: AppSettings = {
  id: "global",
  maintenanceMode: false,
  minimumAppVersion: "2.1.0",
  forceUpdate: false,
  announcementMessage: "6月20日 午前2時から短いメンテナンスを予定しています。",
  updatedAt: daysAgo(2)
};

const featureLabels: Record<FeatureKey, string> = {
  steps: "歩数計",
  notes: "メモ",
  schedule: "予定",
  medicine: "薬アラーム",
  todos: "今日やること",
  shopping: "買い物リスト",
  family: "家族連絡先"
};

const featureTotals: Record<FeatureKey, number> = {
  steps: 1842,
  notes: 986,
  schedule: 812,
  medicine: 746,
  todos: 692,
  shopping: 511,
  family: 284
};

export const mockFeatureUsage: FeatureUsage[] = (
  Object.keys(featureLabels) as FeatureKey[]
).map((feature, index) => ({
  feature,
  label: featureLabels[feature],
  total: featureTotals[feature],
  uniqueUsers: Math.max(2, 11 - index),
  change: [0.12, 0.08, 0.04, 0.15, -0.03, 0.06, 0.02][index]
}));

export const mockFeatureDaily: FeatureDailyMetric[] = Array.from(
  { length: 7 },
  (_, dayIndex) =>
    (Object.keys(featureLabels) as FeatureKey[]).map((feature, featureIndex) => ({
      date: daysAgo(6 - dayIndex).slice(5, 10).replace("-", "/"),
      feature,
      label: featureLabels[feature],
      count: 20 + dayIndex * 5 + (7 - featureIndex) * 4 + ((dayIndex + featureIndex) % 4) * 3
    }))
).flat();

export const mockEvents: UserEvent[] = mockUsers.flatMap((user, userIndex) =>
  (["steps", "schedule", "medicine", "todos"] as FeatureKey[]).map(
    (feature, index) => ({
      id: `${user.id}-${feature}`,
      userId: user.id,
      feature,
      eventName: `${feature}_opened`,
      occurredAt: daysAgo((userIndex + index) % 6, 7 + index * 2),
      appVersion: user.appVersion
    })
  )
);

export const mockDashboard: DashboardData = {
  totalUsers: mockUsers.length,
  activeUsers: mockUsers.filter(
    (user) => new Date(user.lastActiveAt).getTime() >= now.getTime() - 7 * 86400000
  ).length,
  premiumUsers: mockUsers.filter((user) => user.premium).length,
  freeUsers: mockUsers.filter((user) => !user.premium).length,
  activeSubscriptions: mockSubscriptions.filter((sub) => sub.status === "active")
    .length,
  cancelledSubscriptions: mockSubscriptions.filter(
    (sub) => sub.status === "cancelled"
  ).length,
  metrics: Array.from({ length: 14 }, (_, index) => ({
    date: daysAgo(13 - index).slice(5, 10).replace("-", "/"),
    users: 9 + Math.floor(index / 3),
    activeUsers: 5 + (index % 4) + Math.floor(index / 5),
    premiumUsers: 3 + Math.floor(index / 5),
    subscriptions: 2 + Math.floor(index / 6)
  })),
  featureUsage: mockFeatureUsage,
  recentUsers: [...mockUsers]
    .sort(
      (a, b) =>
        new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    )
    .slice(0, 5)
};

export function getMockUserDetail(id: string): UserDetailData | null {
  const user = mockUsers.find((item) => item.id === id);
  if (!user) return null;
  return {
    user,
    subscription: mockSubscriptions.find((item) => item.userId === id),
    recentEvents: mockEvents
      .filter((event) => event.userId === id)
      .sort(
        (a, b) =>
          new Date(b.occurredAt).getTime() - new Date(a.occurredAt).getTime()
      )
  };
}
