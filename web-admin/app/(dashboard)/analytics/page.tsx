import { Activity, ArrowDown, ArrowUp, Users } from "lucide-react";

import {
  FeatureRankingChart,
  FeatureTrendChart
} from "@/components/charts/feature-charts";
import { PageHeader } from "@/components/page-header";
import { Badge } from "@/components/ui/badge";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle
} from "@/components/ui/card";
import { getFeatureAnalytics } from "@/lib/data";
import { formatNumber, formatPercent } from "@/lib/utils";

export const metadata = { title: "機能分析" };
export const dynamic = "force-dynamic";

export default async function AnalyticsPage() {
  const { summary, daily } = await getFeatureAnalytics();
  const top = summary[0];

  return (
    <div className="space-y-6">
      <PageHeader
        title="機能分析"
        description="どの機能が継続利用につながっているかを確認します。"
      />

      <div className="metric-grid grid gap-4">
        <Card>
          <CardContent className="pt-5">
            <Activity className="h-5 w-5 text-primary" />
            <p className="mt-4 text-sm text-muted-foreground">最も使われる機能</p>
            <p className="mt-1 text-2xl font-bold">{top?.label || "データなし"}</p>
            <p className="mt-2 text-xs text-muted-foreground">
              {top ? `${formatNumber(top.total)}回` : "集計待ち"}
            </p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-5">
            <Users className="h-5 w-5 text-[#b08939]" />
            <p className="mt-4 text-sm text-muted-foreground">利用ユーザー</p>
            <p className="mt-1 text-2xl font-bold">
              {formatNumber(
                Math.max(...summary.map((item) => item.uniqueUsers), 0)
              )}
            </p>
            <p className="mt-2 text-xs text-muted-foreground">
              上位機能のユニーク利用者
            </p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="pt-5">
            {top?.change && top.change < 0 ? (
              <ArrowDown className="h-5 w-5 text-red-700" />
            ) : (
              <ArrowUp className="h-5 w-5 text-emerald-700" />
            )}
            <p className="mt-4 text-sm text-muted-foreground">前期間比</p>
            <p className="mt-1 text-2xl font-bold">
              {top ? formatPercent(top.change) : "0%"}
            </p>
            <p className="mt-2 text-xs text-muted-foreground">利用回数の変化</p>
          </CardContent>
        </Card>
      </div>

      <div className="grid gap-4 xl:grid-cols-2">
        <Card>
          <CardHeader>
            <CardTitle>機能別ランキング</CardTitle>
            <CardDescription>期間内の利用回数を比較</CardDescription>
          </CardHeader>
          <CardContent>
            <FeatureRankingChart data={summary} />
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <CardTitle>日別利用推移</CardTitle>
            <CardDescription>最近7日間の主要機能</CardDescription>
          </CardHeader>
          <CardContent>
            <FeatureTrendChart data={daily} />
          </CardContent>
        </Card>
      </div>

      <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
        {summary.map((item) => (
          <Card key={item.feature}>
            <CardContent className="flex items-center justify-between gap-4 pt-5">
              <div>
                <p className="font-bold">{item.label}</p>
                <p className="mt-1 text-sm text-muted-foreground">
                  {formatNumber(item.uniqueUsers)}人が利用
                </p>
              </div>
              <div className="text-right">
                <p className="text-xl font-bold">{formatNumber(item.total)}</p>
                <Badge
                  variant={item.change >= 0 ? "success" : "destructive"}
                  className="mt-2"
                >
                  {item.change >= 0 ? "+" : ""}
                  {formatPercent(item.change)}
                </Badge>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}
