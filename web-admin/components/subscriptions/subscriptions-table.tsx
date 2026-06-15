"use client";

import { Search } from "lucide-react";
import Link from "next/link";
import { useMemo, useState } from "react";

import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow
} from "@/components/ui/table";
import type { Subscription } from "@/lib/types";
import { formatDate } from "@/lib/utils";

const statusLabel = {
  active: "有効",
  expired: "期限切れ",
  cancelled: "解約済み"
};

export function SubscriptionsTable({
  subscriptions
}: {
  subscriptions: Subscription[];
}) {
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState("all");
  const [plan, setPlan] = useState("all");

  const filtered = useMemo(() => {
    const normalized = query.trim().toLowerCase();
    return subscriptions.filter((subscription) => {
      const matchesQuery =
        !normalized ||
        subscription.userName.toLowerCase().includes(normalized) ||
        subscription.productId.toLowerCase().includes(normalized) ||
        subscription.userId.toLowerCase().includes(normalized);
      return (
        matchesQuery &&
        (status === "all" || subscription.status === status) &&
        (plan === "all" || subscription.plan === plan)
      );
    });
  }, [plan, query, status, subscriptions]);

  return (
    <div className="space-y-4">
      <div className="flex flex-col gap-3 lg:flex-row">
        <div className="relative flex-1">
          <Search className="absolute left-3 top-3 h-5 w-5 text-muted-foreground" />
          <Input
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            className="pl-10"
            placeholder="名前、UID、商品IDで検索"
          />
        </div>
        <select
          className="h-11 rounded-md border bg-background px-3 text-sm"
          value={status}
          onChange={(event) => setStatus(event.target.value)}
        >
          <option value="all">全状態</option>
          <option value="active">有効</option>
          <option value="cancelled">解約済み</option>
          <option value="expired">期限切れ</option>
        </select>
        <select
          className="h-11 rounded-md border bg-background px-3 text-sm"
          value={plan}
          onChange={(event) => setPlan(event.target.value)}
        >
          <option value="all">全プラン</option>
          <option value="monthly">月額</option>
          <option value="lifetime">買い切り</option>
        </select>
      </div>

      <div className="overflow-hidden rounded-lg border bg-card">
        <Table>
          <TableHeader>
            <TableRow>
              <TableHead>ユーザー</TableHead>
              <TableHead>状態</TableHead>
              <TableHead>プラン</TableHead>
              <TableHead>商品ID</TableHead>
              <TableHead>開始日</TableHead>
              <TableHead>有効期限</TableHead>
              <TableHead className="text-right">詳細</TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {filtered.map((subscription) => (
              <TableRow key={subscription.id}>
                <TableCell>
                  <p className="font-semibold">{subscription.userName}</p>
                  <p className="mt-1 text-xs text-muted-foreground">
                    {subscription.userId}
                  </p>
                </TableCell>
                <TableCell>
                  <Badge
                    variant={
                      subscription.status === "active"
                        ? "success"
                        : subscription.status === "cancelled"
                          ? "warning"
                          : "destructive"
                    }
                  >
                    {statusLabel[subscription.status]}
                  </Badge>
                </TableCell>
                <TableCell>
                  {subscription.plan === "monthly" ? "月額" : "買い切り"}
                </TableCell>
                <TableCell className="font-mono text-xs">
                  {subscription.productId}
                </TableCell>
                <TableCell>{formatDate(subscription.startedAt)}</TableCell>
                <TableCell>
                  {subscription.plan === "lifetime"
                    ? "無期限"
                    : formatDate(subscription.expiresAt)}
                </TableCell>
                <TableCell className="text-right">
                  <Button asChild variant="outline" size="sm">
                    <Link href={`/users/${subscription.userId}`}>開く</Link>
                  </Button>
                </TableCell>
              </TableRow>
            ))}
            {!filtered.length ? (
              <TableRow>
                <TableCell colSpan={7} className="h-32 text-center text-muted-foreground">
                  条件に合う課金情報はありません。
                </TableCell>
              </TableRow>
            ) : null}
          </TableBody>
        </Table>
      </div>
    </div>
  );
}
