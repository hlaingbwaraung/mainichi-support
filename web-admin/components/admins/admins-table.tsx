"use client";

import { ShieldPlus, Trash2 } from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";

import { removeAdmin, upsertAdmin } from "@/lib/admin-actions";
import type { AdminProfile, AdminRole } from "@/lib/types";
import { formatDate, formatDateTime } from "@/lib/utils";
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

const roleLabel = {
  owner: "オーナー",
  admin: "管理者",
  viewer: "閲覧者"
};

export function AdminsTable({
  initialAdmins,
  currentAdminId
}: {
  initialAdmins: AdminProfile[];
  currentAdminId: string;
}) {
  const [admins, setAdmins] = useState(initialAdmins);
  const [identifier, setIdentifier] = useState("");
  const [role, setRole] = useState<AdminRole>("viewer");
  const [busy, setBusy] = useState(false);

  async function addAdmin(event: React.FormEvent) {
    event.preventDefault();
    if (!identifier.trim()) return;
    setBusy(true);
    try {
      const result = await upsertAdmin(identifier.trim(), role);
      const uid = result.uid || `new-${Date.now()}`;
      setAdmins((current) => [
        ...current.filter((admin) => admin.id !== uid),
        {
          id: uid,
          email: identifier.includes("@") ? identifier : "Firebase Authから取得",
          displayName: "新しい管理者",
          role,
          active: true,
          createdAt: new Date().toISOString()
        }
      ]);
      setIdentifier("");
      toast.success("管理者を追加しました。");
    } catch {
      toast.error("管理者を追加できませんでした。");
    } finally {
      setBusy(false);
    }
  }

  async function remove(uid: string) {
    setBusy(true);
    try {
      await removeAdmin(uid);
      setAdmins((current) => current.filter((admin) => admin.id !== uid));
      toast.success("管理権限を削除しました。");
    } catch {
      toast.error("管理権限を削除できませんでした。");
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="space-y-6">
      <form
        onSubmit={addAdmin}
        className="grid gap-3 border-b pb-6 md:grid-cols-[1fr_180px_auto]"
      >
        <Input
          value={identifier}
          onChange={(event) => setIdentifier(event.target.value)}
          placeholder="Firebase AuthのメールまたはUID"
          aria-label="追加する管理者"
        />
        <select
          value={role}
          onChange={(event) => setRole(event.target.value as AdminRole)}
          className="h-11 rounded-md border bg-background px-3 text-sm"
          aria-label="役割"
        >
          <option value="viewer">閲覧者</option>
          <option value="admin">管理者</option>
          <option value="owner">オーナー</option>
        </select>
        <Button type="submit" disabled={busy}>
          <ShieldPlus className="h-4 w-4" />
          追加
        </Button>
      </form>

      <Table>
        <TableHeader>
          <TableRow>
            <TableHead>管理者</TableHead>
            <TableHead>役割</TableHead>
            <TableHead>追加日</TableHead>
            <TableHead>最終ログイン</TableHead>
            <TableHead className="text-right">操作</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {admins.map((admin) => (
            <TableRow key={admin.id}>
              <TableCell>
                <p className="font-semibold">{admin.displayName}</p>
                <p className="mt-1 text-xs text-muted-foreground">{admin.email}</p>
              </TableCell>
              <TableCell>
                <Badge variant={admin.role === "owner" ? "warning" : "secondary"}>
                  {roleLabel[admin.role]}
                </Badge>
              </TableCell>
              <TableCell>{formatDate(admin.createdAt)}</TableCell>
              <TableCell>{formatDateTime(admin.lastLoginAt)}</TableCell>
              <TableCell className="text-right">
                <Button
                  variant="ghost"
                  size="sm"
                  className="text-red-700"
                  disabled={
                    busy ||
                    admin.id === currentAdminId ||
                    (admin.role === "owner" &&
                      admins.filter((item) => item.role === "owner").length === 1)
                  }
                  onClick={() => remove(admin.id)}
                  title="管理権限を削除"
                >
                  <Trash2 className="h-4 w-4" />
                  削除
                </Button>
              </TableCell>
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  );
}
