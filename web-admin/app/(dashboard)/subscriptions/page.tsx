import { CircleOff, Clock3, CreditCard } from "lucide-react";

import { PageHeader } from "@/components/page-header";
import { StatCard } from "@/components/dashboard/stat-card";
import { SubscriptionsTable } from "@/components/subscriptions/subscriptions-table";
import { getSubscriptions } from "@/lib/data";
import { formatNumber } from "@/lib/utils";

export const metadata = { title: "サブスクリプション" };
export const dynamic = "force-dynamic";

export default async function SubscriptionsPage() {
  const subscriptions = await getSubscriptions();
  const active = subscriptions.filter((item) => item.status === "active").length;
  const cancelled = subscriptions.filter(
    (item) => item.status === "cancelled"
  ).length;
  const monthly = subscriptions.filter((item) => item.plan === "monthly").length;

  return (
    <div className="space-y-6">
      <PageHeader
        title="サブスクリプション"
        description="Google Playで確認した課金状態を管理します。"
      />
      <div className="metric-grid grid gap-4">
        <StatCard
          label="有効"
          value={formatNumber(active)}
          hint="現在プレミアム利用中"
          icon={CreditCard}
          tone="green"
        />
        <StatCard
          label="月額プラン"
          value={formatNumber(monthly)}
          hint="自動更新サブスクリプション"
          icon={Clock3}
          tone="gold"
        />
        <StatCard
          label="解約済み"
          value={formatNumber(cancelled)}
          hint="更新停止、期限までは利用可能"
          icon={CircleOff}
          tone="red"
        />
      </div>
      <SubscriptionsTable subscriptions={subscriptions} />
    </div>
  );
}
