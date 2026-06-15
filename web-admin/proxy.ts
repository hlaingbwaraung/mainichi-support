import { NextRequest, NextResponse } from "next/server";

import { SESSION_COOKIE } from "@/lib/constants";

const protectedRoutes = [
  "/dashboard",
  "/users",
  "/subscriptions",
  "/analytics",
  "/settings",
  "/admins"
];

export function proxy(request: NextRequest) {
  const protectedRoute = protectedRoutes.some((route) =>
    request.nextUrl.pathname.startsWith(route)
  );
  if (
    protectedRoute &&
    !request.cookies.get(SESSION_COOKIE)?.value
  ) {
    return NextResponse.redirect(new URL("/login", request.url));
  }
  return NextResponse.next();
}

export const config = {
  matcher: [
    "/dashboard/:path*",
    "/users/:path*",
    "/subscriptions/:path*",
    "/analytics/:path*",
    "/settings/:path*",
    "/admins/:path*"
  ]
};
