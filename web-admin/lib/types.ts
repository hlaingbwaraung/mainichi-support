export type AdminRole = "owner" | "admin" | "viewer";
export type UserPlan = "free" | "monthly" | "lifetime";
export type SubscriptionStatus = "active" | "expired" | "cancelled";
export type FeatureKey =
  | "steps"
  | "notes"
  | "schedule"
  | "medicine"
  | "todos"
  | "shopping"
  | "family";

export interface AdminProfile {
  id: string;
  email: string;
  displayName: string;
  role: AdminRole;
  active: boolean;
  createdAt: string;
  lastLoginAt?: string;
}

export interface AppUser {
  id: string;
  displayName: string;
  email?: string;
  phone?: string;
  plan: UserPlan;
  premium: boolean;
  disabled: boolean;
  appVersion: string;
  platform: "android";
  createdAt: string;
  lastActiveAt: string;
  stepCountToday: number;
  familyContactCount: number;
}

export interface Subscription {
  id: string;
  userId: string;
  userName: string;
  status: SubscriptionStatus;
  plan: Exclude<UserPlan, "free">;
  productId: string;
  startedAt: string;
  expiresAt?: string;
  cancelledAt?: string;
  source: "google_play";
}

export interface UserEvent {
  id: string;
  userId: string;
  feature: FeatureKey;
  eventName: string;
  occurredAt: string;
  appVersion: string;
}

export interface FeatureUsage {
  feature: FeatureKey;
  label: string;
  total: number;
  uniqueUsers: number;
  change: number;
}

export interface DailyMetric {
  date: string;
  users: number;
  activeUsers: number;
  premiumUsers: number;
  subscriptions: number;
}

export interface FeatureDailyMetric {
  date: string;
  feature: FeatureKey;
  label: string;
  count: number;
}

export interface DashboardData {
  totalUsers: number;
  activeUsers: number;
  premiumUsers: number;
  freeUsers: number;
  activeSubscriptions: number;
  cancelledSubscriptions: number;
  metrics: DailyMetric[];
  featureUsage: FeatureUsage[];
  recentUsers: AppUser[];
}

export interface AppSettings {
  id: "global";
  maintenanceMode: boolean;
  minimumAppVersion: string;
  forceUpdate: boolean;
  announcementMessage: string;
  updatedAt: string;
}

export interface UserDetailData {
  user: AppUser;
  subscription?: Subscription;
  recentEvents: UserEvent[];
}
