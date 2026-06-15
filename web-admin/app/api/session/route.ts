import { NextResponse } from "next/server";

import { SESSION_COOKIE } from "@/lib/auth";
import { adminAuth, adminDb, isMockServer } from "@/lib/firebase/admin";

const expiresIn = 60 * 60 * 24 * 5 * 1000;

export async function POST(request: Request) {
  const { token } = (await request.json()) as { token?: string };
  if (!token) {
    return NextResponse.json(
      { message: "認証トークンがありません。" },
      { status: 400 }
    );
  }

  try {
    let sessionCookie = "mock-session";
    if (!isMockServer) {
      if (!adminAuth || !adminDb) throw new Error("Firebase Admin is unavailable.");
      const decoded = await adminAuth.verifyIdToken(token);
      const adminSnapshot = await adminDb.collection("admins").doc(decoded.uid).get();
      if (!adminSnapshot.exists || !adminSnapshot.data()?.active) {
        return NextResponse.json(
          { message: "このアカウントには管理権限がありません。" },
          { status: 403 }
        );
      }
      sessionCookie = await adminAuth.createSessionCookie(token, { expiresIn });
      await adminSnapshot.ref.update({
        lastLoginAt: new Date(),
        email: decoded.email || adminSnapshot.data()?.email || ""
      });
    }

    const response = NextResponse.json({ ok: true });
    response.cookies.set(SESSION_COOKIE, sessionCookie, {
      httpOnly: true,
      secure: process.env.NODE_ENV === "production",
      sameSite: "lax",
      maxAge: expiresIn / 1000,
      path: "/"
    });
    return response;
  } catch {
    return NextResponse.json(
      { message: "認証情報を確認できませんでした。" },
      { status: 401 }
    );
  }
}

export async function DELETE() {
  const response = NextResponse.json({ ok: true });
  response.cookies.set(SESSION_COOKIE, "", {
    httpOnly: true,
    maxAge: 0,
    path: "/"
  });
  return response;
}
