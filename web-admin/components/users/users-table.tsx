"use client";

import { ArrowDownAZ, Download, Eye, Search, UserX } from "lucide-react";
import Link from "next/link";
import { useMemo, useState } from "react";
import { toast } from "sonner";

import { exportUsersCsv, setUserDisabled } from "@/lib/admin-actions";
import { isMockClient } from "@/lib/firebase/client";
import type { AdminRole, AppUser } from "@/lib/types";
import { formatDate, formatDateTime } from "@/lib/utils";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle
} from "@/components/ui/dialog";
import { Input } from "@/components/ui/input";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow
} from "@/components/ui/table";

function toCsv(users: AppUser[]) {
  const rows = [
    ["UID", "名前", "メール", "プラン", "状態", "登録日", "最終利用", "アプリ版"],
    ...users.map((user) => [
      user.id,
      user.displayName,
      user.email || "",
      user.plan,
      user.disabled ? "停止" : "有効",
      user.createdAt,
      user.lastActiveAt,
      user.appVersion
    ])
  ];
  return rows
    .map((row) =>
      row.map((cell) => `"${String(cell).replaceAll('"', '""')}"`).join(",")
    )
    .join("\n");
}

function downloadCsv(csv: string) {
  const blob = new Blob(["\uFEFF", csv], { type: "text/csv;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = `mainichi-users-${new Date().toISOString().slice(0, 10)}.csv`;
  anchor.click();
  URL.revokeObjectURL(url);
}

export function UsersTable({
  initialUsers,
  role
}: {
  initialUsers: AppUser[];
  role: AdminRole;
}) {
  const [users, setUsers] = useState(initialUsers);
  const [query, setQuery] = useState("");
  const [plan, setPlan] = useState("all");
  const [status, setStatus] = useState("all");
  const [sort, setSort] = useState<"newest" | "name" | "active">("newest");
  const [target, setTarget] = useState<AppUser | null>(null);
  const [busy, setBusy] = useState(false);

  const filtered = useMemo(() => {
    const normalized = query.trim().toLowerCase();
    return [...users]
      .filter((user) => {
        const matchesQuery =
          !normalized ||
          user.displayName.toLowerCase().includes(normalized) ||
          user.email?.toLowerCase().includes(normalized) ||
          user.id.toLowerCase().includes(normalized);
        const matchesPlan =
          plan === "all" ||
          (plan === "premium" ? user.premium : !user.premium);
        const matchesStatus =
          status === "all" ||
          (status === "disabled" ? user.disabled : !user.disabled);
        return matchesQuery && matchesPlan && matchesStatus;
      })
      .sort((a, b) => {
        if (sort === "name") return a.displayName.localeCompare(b.displayName, "ja");
        if (sort === "active") {
          return (
            new Date(b.lastActiveAt).getTime() -
            new Date(a.lastActiveAt).getTime()
          );
        }
        return new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime();
      });
  }, [plan, query, sort, status, users]);

  async function toggleDisabled() {
    if (!target) return;
    setBusy(true);
    try {
      await setUserDisabled(target.id, !target.disabled);
      setUsers((current) =>
        current.map((user) =>
          user.id === target.id ? { ...user, disabled: !user.disabled } : user
        )
      );
      toast.success(target.disabled ? "ユーザーを再開しました。" : "ユーザーを停止しました。");
      setTarget(null);
    } catch {
      toast.error("状態を変更できませんでした。");
    } finally {
      setBusy(false);
    }
  }

  async function exportCsv() {
    try {
      const csv = isMockClient ? toCsv(filtered) : await exportUsersCsv();
      if (!csv) throw new Error();
      downloadCsv(csv);
      toast.success("CSVを書き出しました。");
    } catch {
      toast.error("CSVを書き出せませんでした。");
    }
  }

  return (
    <>
      <div className="space-y-4">
        <div className="flex flex-col gap-3 xl:flex-row xl:items-center">
          <div className="relative min-w-0 flex-1">
            <Search className="pointer-events-none absolute left-3 top-3 h-5 w-5 text-muted-foreground" />
            <Input
              value={query}
              onChange={(event) => setQuery(event.target.value)}
              placeholder="名前、メール、UIDで検索"
              className="pl-10"
            />
          </div>
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 xl:flex">
            <select
              aria-label="プランで絞り込む"
              className="h-11 rounded-md border bg-background px-3 text-sm"
              value={plan}
              onChange={(event) => setPlan(event.target.value)}
            >
              <option value="all">全プラン</option>
              <option value="premium">プレミアム</option>
              <option value="free">無料</option>
            </select>
            <select
              aria-label="状態で絞り込む"
              className="h-11 rounded-md border bg-background px-3 text-sm"
              value={status}
              onChange={(event) => setStatus(event.target.value)}
            >
              <option value="all">全状態</option>
              <option value="active">有効</option>
              <option value="disabled">停止中</option>
            </select>
            <select
              aria-label="並び順"
              className="col-span-2 h-11 rounded-md border bg-background px-3 text-sm sm:col-span-1"
              value={sort}
              onChange={(event) => setSort(event.target.value as typeof sort)}
            >
              <option value="newest">登録が新しい順</option>
              <option value="active">最近利用した順</option>
              <option value="name">名前順</option>
            </select>
          </div>
          {role !== "viewer" ? (
            <Button variant="outline" onClick={exportCsv}>
              <Download className="h-4 w-4" />
              CSV
            </Button>
          ) : null}
        </div>

        <div className="flex items-center gap-2 text-sm text-muted-foreground">
          <ArrowDownAZ className="h-4 w-4" />
          {filtered.length}人を表示
        </div>

        <div className="overflow-hidden rounded-lg border bg-card">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>ユーザー</TableHead>
                <TableHead>プラン</TableHead>
                <TableHead>状態</TableHead>
                <TableHead>最終利用</TableHead>
                <TableHead>登録日</TableHead>
                <TableHead className="text-right">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {filtered.map((user) => (
                <TableRow key={user.id}>
                  <TableCell>
                    <p className="font-semibold">{user.displayName}</p>
                    <p className="mt-1 text-xs text-muted-foreground">
                      {user.email || user.id}
                    </p>
                  </TableCell>
                  <TableCell>
                    <Badge variant={user.premium ? "warning" : "secondary"}>
                      {user.plan === "monthly"
                        ? "月額"
                        : user.plan === "lifetime"
                          ? "買い切り"
                          : "無料"}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    <Badge variant={user.disabled ? "destructive" : "success"}>
                      {user.disabled ? "停止中" : "有効"}
                    </Badge>
                  </TableCell>
                  <TableCell className="whitespace-nowrap">
                    {formatDateTime(user.lastActiveAt)}
                  </TableCell>
                  <TableCell className="whitespace-nowrap">
                    {formatDate(user.createdAt)}
                  </TableCell>
                  <TableCell>
                    <div className="flex justify-end gap-2">
                      <Button asChild variant="outline" size="sm" title="詳細">
                        <Link href={`/users/${user.id}`}>
                          <Eye className="h-4 w-4" />
                          <span className="hidden sm:inline">詳細</span>
                        </Link>
                      </Button>
                      {role !== "viewer" ? (
                        <Button
                          variant="ghost"
                          size="sm"
                          className={user.disabled ? "text-emerald-700" : "text-red-700"}
                          onClick={() => setTarget(user)}
                        >
                          <UserX className="h-4 w-4" />
                          <span className="hidden xl:inline">
                            {user.disabled ? "再開" : "停止"}
                          </span>
                        </Button>
                      ) : null}
                    </div>
                  </TableCell>
                </TableRow>
              ))}
              {!filtered.length ? (
                <TableRow>
                  <TableCell colSpan={6} className="h-32 text-center text-muted-foreground">
                    条件に合うユーザーはいません。
                  </TableCell>
                </TableRow>
              ) : null}
            </TableBody>
          </Table>
        </div>
      </div>

      <Dialog open={Boolean(target)} onOpenChange={(open) => !open && setTarget(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>
              {target?.disabled ? "利用を再開しますか？" : "利用を停止しますか？"}
            </DialogTitle>
            <DialogDescription>
              {target?.displayName}さんのアカウントを
              {target?.disabled ? "再び利用できる状態に戻します。" : "ログインできない状態にします。データは削除されません。"}
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <DialogClose asChild>
              <Button variant="outline">戻る</Button>
            </DialogClose>
            <Button
              variant={target?.disabled ? "default" : "destructive"}
              onClick={toggleDisabled}
              disabled={busy}
            >
              {busy ? "変更中..." : target?.disabled ? "利用を再開" : "利用を停止"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </>
  );
}
