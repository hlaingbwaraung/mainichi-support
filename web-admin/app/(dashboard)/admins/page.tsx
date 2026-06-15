import { redirect } from "next/navigation";

import { AdminsTable } from "@/components/admins/admins-table";
import { PageHeader } from "@/components/page-header";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle
} from "@/components/ui/card";
import { requireAdmin } from "@/lib/auth";
import { getAdmins } from "@/lib/data";

export const metadata = { title: "管理者" };
export const dynamic = "force-dynamic";

export default async function AdminsPage() {
  const [admins, currentAdmin] = await Promise.all([getAdmins(), requireAdmin()]);
  if (currentAdmin.role !== "owner") redirect("/dashboard");

  return (
    <div className="space-y-6">
      <PageHeader
        title="管理者"
        description="管理画面へアクセスできる人と役割を管理します。"
      />
      <Card>
        <CardHeader>
          <CardTitle>管理者アカウント</CardTitle>
          <CardDescription>
            オーナーのみ追加、役割変更、削除ができます。
          </CardDescription>
        </CardHeader>
        <CardContent>
          <AdminsTable
            initialAdmins={admins}
            currentAdminId={currentAdmin.id}
          />
        </CardContent>
      </Card>
    </div>
  );
}
