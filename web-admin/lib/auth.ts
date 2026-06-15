import "server-only";

import { cookies } from "next/headers";
import { redirect } from "next/navigation";

import { adminAuth, adminDb, isMockServer } from "@/lib/firebase/admin";
import { SESSION_COOKIE } from "@/lib/constants";
import { mockAdmins } from "@/lib/mock-data";
import type { AdminProfile } from "@/lib/types";

export { SESSION_COOKIE };

export async function getSessionAdmin(): Promise<AdminProfile | null> {
  const cookieStore = await cookies();
  const sessionCookie = cookieStore.get(SESSION_COOKIE)?.value;

  if (isMockServer) {
    return sessionCookie ? mockAdmins[0] : null;
  }
  if (!sessionCookie || !adminAuth || !adminDb) return null;

  try {
    const decoded = await adminAuth.verifySessionCookie(sessionCookie, true);
    const adminSnapshot = await adminDb.collection("admins").doc(decoded.uid).get();
    if (!adminSnapshot.exists) return null;
    const data = adminSnapshot.data();
    if (!data?.active) return null;
    return {
      id: decoded.uid,
      email: decoded.email || data.email || "",
      displayName: data.displayName || decoded.name || "管理者",
      role: data.role,
      active: data.active,
      createdAt: data.createdAt?.toDate?.().toISOString() || new Date().toISOString(),
      lastLoginAt:
        data.lastLoginAt?.toDate?.().toISOString() || new Date().toISOString()
    };
  } catch {
    return null;
  }
}

export async function requireAdmin() {
  const admin = await getSessionAdmin();
  if (!admin) redirect("/login");
  return admin;
}
