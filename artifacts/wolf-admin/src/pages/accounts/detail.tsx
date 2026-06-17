import { useParams, Link } from "wouter";
import { useState } from "react";
import { useGetAccount, useGetAccountCache, useClearAccountCache, getGetAccountCacheQueryKey } from "@workspace/api-client-react";
import { useQueryClient } from "@tanstack/react-query";
import { ArrowLeft, RefreshCw, Eye, EyeOff, Server, Database, Clock, Zap, ShieldAlert, KeyRound, Globe, Film, Tv, Radio } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Skeleton } from "@/components/ui/skeleton";
import { useToast } from "@/hooks/use-toast";
import { AccountDialog } from "@/components/accounts/account-dialog";

export default function AccountDetail() {
  const { id } = useParams<{ id: string }>();
  const accountId = parseInt(id || "0", 10);
  const [showPassword, setShowPassword] = useState(false);
  const { toast } = useToast();
  const queryClient = useQueryClient();

  const { data: account, isLoading: accountLoading } = useGetAccount(accountId);
  const { data: cache, isLoading: cacheLoading } = useGetAccountCache(accountId);
  const clearCache = useClearAccountCache();

  const handleClearCache = () => {
    clearCache.mutate(
      { id: accountId },
      {
        onSuccess: () => {
          queryClient.invalidateQueries({ queryKey: getGetAccountCacheQueryKey(accountId) });
          toast({
            title: "Cache Flushed",
            description: "Node cache has been forcibly invalidated.",
          });
        },
        onError: () => {
          toast({
            title: "Operation Failed",
            description: "Could not flush cache for this node.",
            variant: "destructive",
          });
        }
      }
    );
  };

  if (accountLoading) {
    return <div className="p-8 max-w-7xl mx-auto"><Skeleton className="h-96 w-full" /></div>;
  }

  if (!account) {
    return <div className="p-8 max-w-7xl mx-auto">Node not found.</div>;
  }

  return (
    <div className="p-8 max-w-7xl mx-auto space-y-8">
      <div className="flex items-center gap-4">
        <Link href="/accounts" className="text-muted-foreground hover:text-foreground transition-colors">
          <ArrowLeft className="w-5 h-5" />
        </Link>
        <div className="flex-1">
          <div className="flex items-center gap-3">
            <h1 className="text-3xl font-bold tracking-tight">{account.name}</h1>
            <Badge variant="outline" className={`font-mono text-[10px] uppercase tracking-wider ${account.type === 'xtream' ? 'text-blue-400 border-blue-400/30 bg-blue-400/10' : 'text-purple-400 border-purple-400/30 bg-purple-400/10'}`}>
              {account.type}
            </Badge>
            {account.active ? (
              <Badge variant="outline" className="text-emerald-400 border-emerald-400/30 bg-emerald-400/10 font-mono text-[10px] uppercase tracking-wider">Online</Badge>
            ) : (
              <Badge variant="outline" className="text-muted-foreground border-muted-foreground/30 font-mono text-[10px] uppercase tracking-wider">Offline</Badge>
            )}
          </div>
          <p className="text-muted-foreground font-mono text-xs mt-1 uppercase tracking-widest">ID: {String(account.id).padStart(6, '0')}</p>
        </div>
        <AccountDialog accountToEdit={account}>
          <Button variant="outline" className="font-mono uppercase tracking-wider text-xs border-border/50">
            Edit Node
          </Button>
        </AccountDialog>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <Card className="lg:col-span-2 bg-card/40 border-border/50 backdrop-blur-sm">
          <CardHeader className="border-b border-border/20 pb-4">
            <CardTitle className="text-sm font-medium text-muted-foreground uppercase tracking-wider flex items-center gap-2">
              <Server className="w-4 h-4 text-primary" />
              Connection Parameters
            </CardTitle>
          </CardHeader>
          <CardContent className="pt-6">
            <div className="space-y-6">
              {account.type === 'xtream' ? (
                <>
                  <div className="space-y-1">
                    <label className="text-[10px] font-mono uppercase text-muted-foreground tracking-widest">Endpoint Host</label>
                    <div className="flex items-center gap-2 bg-black/40 p-3 rounded-md border border-white/5 font-mono text-sm">
                      <Globe className="w-4 h-4 text-muted-foreground" />
                      {account.host || '—'}
                    </div>
                  </div>
                  <div className="grid grid-cols-2 gap-4">
                    <div className="space-y-1">
                      <label className="text-[10px] font-mono uppercase text-muted-foreground tracking-widest">Username</label>
                      <div className="flex items-center gap-2 bg-black/40 p-3 rounded-md border border-white/5 font-mono text-sm">
                        <KeyRound className="w-4 h-4 text-muted-foreground" />
                        {account.user || '—'}
                      </div>
                    </div>
                    <div className="space-y-1">
                      <label className="text-[10px] font-mono uppercase text-muted-foreground tracking-widest">Access Token</label>
                      <div className="flex items-center gap-2 bg-black/40 p-3 rounded-md border border-white/5 font-mono text-sm justify-between">
                        <div className="flex items-center gap-2">
                          <ShieldAlert className="w-4 h-4 text-muted-foreground" />
                          <span className={showPassword ? "" : "text-muted-foreground"}>
                            {showPassword ? (account.pass || '—') : '••••••••••••••••'}
                          </span>
                        </div>
                        <Button 
                          variant="ghost" 
                          size="icon" 
                          className="h-6 w-6 text-muted-foreground hover:text-foreground"
                          onClick={() => setShowPassword(!showPassword)}
                        >
                          {showPassword ? <EyeOff className="h-3 w-3" /> : <Eye className="h-3 w-3" />}
                        </Button>
                      </div>
                    </div>
                  </div>
                </>
              ) : (
                <div className="space-y-1">
                  <label className="text-[10px] font-mono uppercase text-muted-foreground tracking-widest">M3U Payload URL</label>
                  <div className="flex items-center gap-2 bg-black/40 p-3 rounded-md border border-white/5 font-mono text-sm break-all">
                    <Globe className="w-4 h-4 text-muted-foreground shrink-0" />
                    {account.url || '—'}
                  </div>
                </div>
              )}
            </div>
          </CardContent>
        </Card>

        <Card className="bg-card/40 border-border/50 backdrop-blur-sm flex flex-col">
          <CardHeader className="border-b border-border/20 pb-4">
            <CardTitle className="text-sm font-medium text-muted-foreground uppercase tracking-wider flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Database className="w-4 h-4 text-primary" />
                Cache Status
              </div>
              <Badge variant="outline" className={`font-mono text-[10px] ${cache?.isExpired ? 'text-destructive border-destructive/30 bg-destructive/10' : 'text-emerald-400 border-emerald-400/30 bg-emerald-400/10'}`}>
                {cacheLoading ? '...' : cache?.isExpired ? 'STALE' : 'SYNCED'}
              </Badge>
            </CardTitle>
          </CardHeader>
          <CardContent className="pt-6 flex-1 flex flex-col">
            {cacheLoading ? (
              <div className="space-y-4">
                <Skeleton className="h-10 w-full" />
                <Skeleton className="h-10 w-full" />
                <Skeleton className="h-10 w-full" />
              </div>
            ) : (
              <div className="space-y-6 flex-1">
                <div className="grid grid-cols-3 gap-2">
                  <div className={`p-3 rounded border text-center flex flex-col items-center justify-center gap-1 ${cache?.hasLive ? 'border-primary/30 bg-primary/5 text-primary' : 'border-border/50 bg-black/20 text-muted-foreground'}`}>
                    <Tv className="w-4 h-4 mb-1" />
                    <span className="text-[10px] font-mono uppercase tracking-wider">Live</span>
                  </div>
                  <div className={`p-3 rounded border text-center flex flex-col items-center justify-center gap-1 ${cache?.hasMovies ? 'border-primary/30 bg-primary/5 text-primary' : 'border-border/50 bg-black/20 text-muted-foreground'}`}>
                    <Film className="w-4 h-4 mb-1" />
                    <span className="text-[10px] font-mono uppercase tracking-wider">VOD</span>
                  </div>
                  <div className={`p-3 rounded border text-center flex flex-col items-center justify-center gap-1 ${cache?.hasSeries ? 'border-primary/30 bg-primary/5 text-primary' : 'border-border/50 bg-black/20 text-muted-foreground'}`}>
                    <Radio className="w-4 h-4 mb-1" />
                    <span className="text-[10px] font-mono uppercase tracking-wider">Series</span>
                  </div>
                </div>

                <div className="bg-black/40 p-3 rounded-md border border-white/5 flex items-center justify-between font-mono text-xs">
                  <span className="text-muted-foreground flex items-center gap-2"><Clock className="w-3 h-3" /> TTL Remaining</span>
                  <span className={cache?.hoursRemaining && cache.hoursRemaining < 2 ? "text-amber-400" : "text-emerald-400"}>
                    {cache?.hoursRemaining !== null ? `${cache?.hoursRemaining.toFixed(1)}h` : '—'}
                  </span>
                </div>
              </div>
            )}
            
            <div className="mt-6 pt-4 border-t border-border/20">
              <Button 
                variant="destructive" 
                className="w-full font-mono uppercase tracking-wider text-xs" 
                onClick={handleClearCache}
                disabled={clearCache.isPending}
              >
                {clearCache.isPending ? <RefreshCw className="w-4 h-4 mr-2 animate-spin" /> : <Zap className="w-4 h-4 mr-2" />}
                Force Invalidate Cache
              </Button>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
