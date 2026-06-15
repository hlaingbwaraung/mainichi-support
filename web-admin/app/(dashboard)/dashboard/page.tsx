import {
  BadgeJapaneseYen,
  CircleOff,
  Crown,
  UserCheck,
  Users
} from "lucide-react";
import Link from "next/link";

import {
  FeatureUsageChart,
  PlanMixChart,
  UserGrowthChart
} from "@/components/charts/overview-charts";
import { StatCard } from "@/components/dashboard/stat-card";
import { PageHeader } from "@/components/page-header";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle
} from "@/components/ui/card";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow
} from "@/components/ui/table";
import { getDashboardData } from "@/lib/data";
import { formatDate, formatNumber } from "@/lib/utils";

export const metadata = { title: "概要" };
export const dynamic = "force-dynamic";

export default async function DashboardPage() {
  const data = await getDashboardData();

  return (
    <div className="space-y-6">
      <PageHeader
        title="ダッシュボード概要"
        description="ユーザー、課金、機能利用の現在地を確認します。"
        actions={
          <Button asChild variant="outline">
            <Link href="/users">ユーザーを見る</Link>
          </Button>
        }
      />

      <div className="metric-grid grid gap-4">
        <StatCard
          label="総ユーザー"
          value={formatNumber(data.totalUsers)}
          hint="登録済みアカウント"
          icon={Users}
        />
        <StatCard
          label="7日アクティブ"
          value={formatNumber(data.activeUsers)}
          hint="過去7日以内に利用"
          icon={UserCheck}
          tone="green"
        />
        <StatCard
          label="プレミアム"
          value={formatNumber(data.premiumUsers)}
          hint={`${data.activeSubscriptions}件の有効な課金`}
          icon={Crown}
          tone="gold"
        />
        <StatCard
          label="無料ユーザー"
          value={formatNumber(data.freeUsers)}
          hint="アップグレード対象"
          icon={BadgeJapaneseYen}
          tone="teal"
        />
        <StatCard
          label="キャンセル"
          value={formatNumber(data.cancelledSubscriptions)}
          hint="解約済みサブスクリプション"
          icon={CircleOff}
          tone="red"
        />
      </div>

      <div className="grid gap-4 xl:grid-cols-[1.6fr_1fr]">
        <Card>
          <CardHeader>
            <CardTitle>ユーザー推移</CardTitle>
            <CardDescription>総ユーザーと7日アクティブユーザー</CardDescription>
          </CardHeader>
          <CardContent>
            <UserGrowthChart data={data.metrics} />
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <CardTitle>会員構成</CardTitle>
            <CardDescription>無料とプレミアムの割合</CardDescription>
          </CardHeader>
          <CardContent>
            <PlanMixChart premium={data.premiumUsers} free={data.freeUsers} />
          </CardContent>
        </Card>
      </div>

      <div className="grid gap-4 xl:grid-cols-[1.2fr_1fr]">
        <Card>
          <CardHeader>
            <CardTitle>よく使われる機能</CardTitle>
            <CardDescription>集計期間内の機能利用回数</CardDescription>
          </CardHeader>
          <CardContent>
            <FeatureUsageChart data={data.featureUsage} />
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex-row items-center justify-between">
            <div>
              <CardTitle>最近の登録</CardTitle>
              <CardDescription>新しく利用を始めたユーザー</CardDescription>
            </div>
            <Button asChild variant="ghost" size="sm">
              <Link href="/users">すべて見る</Link>
            </Button>
          </CardHeader>
          <CardContent className="px-0">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>名前</TableHead>
                  <TableHead>プラン</TableHead>
                  <TableHead>登録日</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {data.recentUsers.map((user) => (
                  <TableRow key={user.id}>
                    <TableCell>
                      <Link
                        href={`/users/${user.id}`}
                        className="font-semibold hover:underline"
                      >
                        {user.displayName}
                      </Link>
                    </TableCell>
                    <TableCell>
                      <Badge variant={user.premium ? "warning" : "secondary"}>
                        {user.premium ? "プレミアム" : "無料"}
                      </Badge>
                    </TableCell>
                    <TableCell className="text-muted-foreground">
                      {formatDate(user.createdAt)}
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
