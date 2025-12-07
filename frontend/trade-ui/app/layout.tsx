import type { Metadata } from "next";
import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";
import { Providers } from "./providers";
import Image from "next/image";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "Trade Platform",
  description: "Trade Platform - Simulator, Audit, Dashboard, and Chatbot",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body
        className={`${geistSans.variable} ${geistMono.variable} antialiased`}
      >                    
      {/* Desktop Navigation */}
        <div className="hidden lg:grid lg:grid-cols-1 mb-4">
          <div className="flex gap-3 items-center justify-end">
            <a href="https://mukizone.com/" target="_self" rel="noopener noreferrer">
              <Image className="dark:invert" src="/mukizone-com.svg" alt="MukiZone" width={140} height={100} priority />
            </a>
          </div>
        </div>
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
