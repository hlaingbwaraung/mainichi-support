import { PageHeader } from "@/components/page-header";
import { UsersTable } from "@/components/users/users-table";
import { requireAdmin } from "@/lib/auth";
import { getUsers } from "@/lib/data";

export const metadata = { title: "ユーザー" };
export const dynamic = "force-dynamic";

export default async function UsersPage() {
  const [users, admin] = await Promise.all([getUsers(), requireAdmin()]);
  return (
    <div className="space-y-6">
      <PageHeader
        title="ユーザー"
        description="検索、絞り込み、詳細確認、利用停止を行えます。"
      />
      <UsersTable initialUsers={users} role={admin.role} />
    </div>
  );
}
