import { ArrowLeft, Smartphone, UserRound } from "lucide-react";
import Link from "next/link";
import { notFound } from "next/navigation";

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
import { UserStatusAction } from "@/components/users/user-status-action";
import { requireAdmin } from "@/lib/auth";
import { getUserDetail } from "@/lib/data";
import { formatDate, formatDateTime, formatNumber } from "@/lib/utils";

export const dynamic = "force-dynamic";

export default async function UserDetailPage({
  params
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  const [detail, admin] = await Promise.all([getUserDetail(id), requireAdmin()]);
  if (!detail) notFound();

  const { user, subscription, recentEvents } = detail;

  return (
    <div className="space-y-6">
      <div>
        <Button asChild variant="ghost" size="sm" className="mb-3">
          <Link href="/users">
            <ArrowLeft className="h-4 w-4" />
            ユーザー一覧
          </Link>
        </Button>
        <PageHeader
          title={user.displayName}
          description={`UID: ${user.id}`}
          actions={
            admin.role !== "viewer" ? (
              <UserStatusAction
                userId={user.id}
                initiallyDisabled={user.disabled}
              />
            ) : undefined
          }
        />
      </div>

      <div className="grid gap-4 lg:grid-cols-3">
        <Card className="lg:col-span-2">
          <CardHeader>
            <CardTitle>基本情報</CardTitle>
            <CardDescription>アカウントと端末の情報</CardDescription>
          </CardHeader>
          <CardContent className="grid gap-5 sm:grid-cols-2">
            <Info label="メール" value={user.email || "未登録"} />
            <Info label="電話番号" value={user.phone || "未登録"} />
            <Info label="登録日" value={formatDate(user.createdAt)} />
            <Info label="最終利用" value={formatDateTime(user.lastActiveAt)} />
            <Info label="アプリ版" value={user.appVersion} />
            <Info label="家族連絡先" value={`${user.familyContactCount}人`} />
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <CardTitle>利用状況</CardTitle>
          </CardHeader>
          <CardContent className="space-y-5">
            <div>
              <p className="text-sm text-muted-foreground">プラン</p>
              <Badge variant={user.premium ? "warning" : "secondary"} className="mt-2">
                {user.plan === "monthly"
                  ? "月額プレミアム"
                  : user.plan === "lifetime"
                    ? "買い切りプレミアム"
                    : "無料"}
              </Badge>
            </div>
            <div>
              <p className="text-sm text-muted-foreground">今日の歩数</p>
              <p className="mt-1 text-3xl font-bold">
                {formatNumber(user.stepCountToday)}
                <span className="ml-1 text-sm font-medium text-muted-foreground">歩</span>
              </p>
            </div>
            <div className="flex items-center gap-2 text-sm text-muted-foreground">
              <Smartphone className="h-4 w-4" />
              Android
            </div>
            <div className="flex items-center gap-2">
              <UserRound className="h-4 w-4" />
              <Badge variant={user.disabled ? "destructive" : "success"}>
                {user.disabled ? "停止中" : "有効"}
              </Badge>
            </div>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>サブスクリプション</CardTitle>
          <CardDescription>Google Playで検証された課金状態</CardDescription>
        </CardHeader>
        <CardContent>
          {subscription ? (
            <div className="grid gap-5 md:grid-cols-4">
              <Info label="状態" value={subscription.status} />
              <Info label="商品ID" value={subscription.productId} />
              <Info label="開始日" value={formatDate(subscription.startedAt)} />
              <Info
                label="有効期限"
                value={
                  subscription.plan === "lifetime"
                    ? "無期限"
                    : formatDate(subscription.expiresAt)
                }
              />
            </div>
          ) : (
            <p className="text-sm text-muted-foreground">
              サブスクリプションはありません。
            </p>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>最近の操作</CardTitle>
          <CardDescription>アプリから送信された利用イベント</CardDescription>
        </CardHeader>
        <CardContent className="px-0">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>日時</TableHead>
                <TableHead>機能</TableHead>
                <TableHead>イベント</TableHead>
                <TableHead>アプリ版</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {recentEvents.map((event) => (
                <TableRow key={event.id}>
                  <TableCell>{formatDateTime(event.occurredAt)}</TableCell>
                  <TableCell>{event.feature}</TableCell>
                  <TableCell className="font-mono text-xs">
                    {event.eventName}
                  </TableCell>
                  <TableCell>{event.appVersion}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </CardContent>
      </Card>
    </div>
  );
}

function Info({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <p className="text-xs font-semibold uppercase text-muted-foreground">
        {label}
      </p>
      <p className="mt-1 break-words font-semibold">{value}</p>
    </div>
  );
}
