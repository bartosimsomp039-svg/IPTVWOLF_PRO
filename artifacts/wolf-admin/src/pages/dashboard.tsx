import { useGetStats } from "@workspace/api-client-react";
import { Activity, Server, Database, CheckCircle2, XCircle } from "lucide-react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Skeleton } from "@/components/ui/skeleton";

export default function Dashboard() {
  const { data: stats, isLoading } = useGetStats();

  return (
    <div className="p-8 max-w-7xl mx-auto space-y-8">
      <div className="flex flex-col gap-2">
        <h1 className="text-3xl font-bold tracking-tight">System Status</h1>
        <p className="text-muted-foreground font-mono text-sm">Real-time overview of active nodes and cache health.</p>
      </div>

      {isLoading || !stats ? (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <Skeleton className="h-32 rounded-lg" />
          <Skeleton className="h-32 rounded-lg" />
          <Skeleton className="h-32 rounded-lg" />
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <Card className="bg-card/50 border-border/50">
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground uppercase tracking-wider">Total Accounts</CardTitle>
              <Server className="h-4 w-4 text-primary" />
            </CardHeader>
            <CardContent>
              <div className="text-4xl font-bold font-mono">{stats.totalAccounts}</div>
              <div className="mt-2 text-xs text-muted-foreground flex items-center gap-2">
                <span className="text-emerald-500 flex items-center gap-1"><CheckCircle2 className="w-3 h-3" /> {stats.activeAccounts} active</span>
                <span className="text-muted-foreground/50">|</span>
                <span>{stats.totalAccounts - stats.activeAccounts} inactive</span>
              </div>
            </CardContent>
          </Card>

          <Card className="bg-card/50 border-border/50">
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground uppercase tracking-wider">Protocol Types</CardTitle>
              <Activity className="h-4 w-4 text-primary" />
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-2 gap-4 mt-2">
                <div>
                  <div className="text-2xl font-bold font-mono text-blue-400">{stats.xtreamCount}</div>
                  <div className="text-xs text-muted-foreground uppercase mt-1">Xtream</div>
                </div>
                <div>
                  <div className="text-2xl font-bold font-mono text-purple-400">{stats.m3uCount}</div>
                  <div className="text-xs text-muted-foreground uppercase mt-1">M3U</div>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card className="bg-card/50 border-border/50">
            <CardHeader className="flex flex-row items-center justify-between pb-2">
              <CardTitle className="text-sm font-medium text-muted-foreground uppercase tracking-wider">Cache Health</CardTitle>
              <Database className="h-4 w-4 text-primary" />
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-2 gap-4 mt-2">
                <div>
                  <div className="text-2xl font-bold font-mono text-emerald-500">{stats.cacheActiveCount}</div>
                  <div className="text-xs text-emerald-500/70 uppercase mt-1 flex items-center gap-1">Valid</div>
                </div>
                <div>
                  <div className="text-2xl font-bold font-mono text-destructive">{stats.cacheExpiredCount}</div>
                  <div className="text-xs text-destructive/70 uppercase mt-1 flex items-center gap-1">Expired</div>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      )}
    </div>
  );
}
