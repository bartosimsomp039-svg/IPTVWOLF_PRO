import { useState } from "react";
import { Link } from "wouter";
import { useListAccounts, useUpdateAccount, useDeleteAccount, getListAccountsQueryKey, getGetStatsQueryKey } from "@workspace/api-client-react";
import { useQueryClient } from "@tanstack/react-query";
import { Plus, MoreHorizontal, Edit, Trash, Activity, Search, ShieldAlert, ShieldCheck } from "lucide-react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import { Switch } from "@/components/ui/switch";
import { DropdownMenu, DropdownMenuContent, DropdownMenuItem, DropdownMenuLabel, DropdownMenuSeparator, DropdownMenuTrigger } from "@/components/ui/dropdown-menu";
import { AccountDialog } from "@/components/accounts/account-dialog";
import { AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogTitle, AlertDialogTrigger } from "@/components/ui/alert-dialog";
import { useToast } from "@/hooks/use-toast";

export default function AccountsList() {
  const { data: accounts, isLoading } = useListAccounts();
  const updateAccount = useUpdateAccount();
  const deleteAccount = useDeleteAccount();
  const queryClient = useQueryClient();
  const { toast } = useToast();

  const handleToggleActive = (id: number, currentStatus: boolean) => {
    updateAccount.mutate(
      { id, data: { active: !currentStatus } },
      {
        onSuccess: () => {
          queryClient.invalidateQueries({ queryKey: getListAccountsQueryKey() });
          queryClient.invalidateQueries({ queryKey: getGetStatsQueryKey() });
        }
      }
    );
  };

  const handleDelete = (id: number) => {
    deleteAccount.mutate(
      { id },
      {
        onSuccess: () => {
          queryClient.invalidateQueries({ queryKey: getListAccountsQueryKey() });
          queryClient.invalidateQueries({ queryKey: getGetStatsQueryKey() });
          toast({
            title: "Node Terminated",
            description: "The configuration has been permanently deleted.",
          });
        }
      }
    );
  };

  return (
    <div className="p-8 max-w-7xl mx-auto space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Node Configurations</h1>
          <p className="text-muted-foreground font-mono text-sm mt-1">Manage IPTV provider details and sync states.</p>
        </div>
        <AccountDialog>
          <Button className="font-mono uppercase tracking-wider text-xs">
            <Plus className="w-4 h-4 mr-2" />
            Add Node
          </Button>
        </AccountDialog>
      </div>

      <div className="bg-card/30 border border-border/50 rounded-lg overflow-hidden backdrop-blur-sm">
        <div className="p-4 border-b border-border/50 flex items-center justify-between">
          <div className="relative w-64">
            <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
            <Input placeholder="Filter by host..." className="pl-9 bg-background/50 border-border/50 font-mono text-sm" />
          </div>
        </div>
        <Table>
          <TableHeader className="bg-black/20">
            <TableRow className="hover:bg-transparent">
              <TableHead className="w-[100px] text-xs font-mono uppercase tracking-wider">Status</TableHead>
              <TableHead className="font-mono text-xs uppercase tracking-wider">Alias / Host</TableHead>
              <TableHead className="font-mono text-xs uppercase tracking-wider">Protocol</TableHead>
              <TableHead className="font-mono text-xs uppercase tracking-wider">Last Sync</TableHead>
              <TableHead className="w-[50px]"></TableHead>
            </TableRow>
          </TableHeader>
          <TableBody>
            {isLoading ? (
              <TableRow>
                <TableCell colSpan={5} className="h-24 text-center text-muted-foreground font-mono">Loading nodes...</TableCell>
              </TableRow>
            ) : accounts?.length === 0 ? (
              <TableRow>
                <TableCell colSpan={5} className="h-24 text-center text-muted-foreground font-mono">No nodes configured.</TableCell>
              </TableRow>
            ) : (
              accounts?.map((account) => (
                <TableRow key={account.id} className="hover:bg-white/5 transition-colors group">
                  <TableCell>
                    <Switch 
                      checked={account.active} 
                      onCheckedChange={() => handleToggleActive(account.id, account.active ?? false)}
                      className="data-[state=checked]:bg-emerald-500"
                    />
                  </TableCell>
                  <TableCell>
                    <Link href={`/accounts/${account.id}`} className="block">
                      <div className="font-medium text-foreground group-hover:text-primary transition-colors">{account.name}</div>
                      <div className="text-xs text-muted-foreground font-mono truncate max-w-[300px]">
                        {account.host || account.url || 'No host set'}
                      </div>
                    </Link>
                  </TableCell>
                  <TableCell>
                    <Badge variant="outline" className={`font-mono text-[10px] uppercase tracking-wider ${account.type === 'xtream' ? 'text-blue-400 border-blue-400/30 bg-blue-400/10' : 'text-purple-400 border-purple-400/30 bg-purple-400/10'}`}>
                      {account.type}
                    </Badge>
                  </TableCell>
                  <TableCell>
                    <div className="text-xs font-mono text-muted-foreground">
                      {account.cacheUpdatedAt ? new Date(account.cacheUpdatedAt).toLocaleString() : 'Never'}
                    </div>
                  </TableCell>
                  <TableCell>
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" className="h-8 w-8 p-0">
                          <span className="sr-only">Open menu</span>
                          <MoreHorizontal className="h-4 w-4" />
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end" className="bg-card border-border">
                        <DropdownMenuLabel className="font-mono text-xs uppercase text-muted-foreground tracking-wider">Actions</DropdownMenuLabel>
                        <DropdownMenuSeparator className="bg-border/50" />
                        <AccountDialog accountToEdit={account}>
                          <div className="relative flex cursor-default select-none items-center rounded-sm px-2 py-1.5 text-sm outline-none transition-colors hover:bg-accent hover:text-accent-foreground data-[disabled]:pointer-events-none data-[disabled]:opacity-50">
                            <Edit className="w-4 h-4 mr-2" />
                            Edit Configuration
                          </div>
                        </AccountDialog>
                        <DropdownMenuSeparator className="bg-border/50" />
                        <AlertDialog>
                          <AlertDialogTrigger asChild>
                            <DropdownMenuItem onSelect={(e) => e.preventDefault()} className="text-destructive focus:text-destructive focus:bg-destructive/10 cursor-pointer">
                              <Trash className="w-4 h-4 mr-2" />
                              Terminate Node
                            </DropdownMenuItem>
                          </AlertDialogTrigger>
                          <AlertDialogContent className="bg-card border-destructive/30">
                            <AlertDialogHeader>
                              <AlertDialogTitle className="font-mono uppercase tracking-wider text-destructive">Terminate Node Configuration</AlertDialogTitle>
                              <AlertDialogDescription className="font-mono text-xs">
                                Are you sure you want to delete this configuration? This action cannot be undone and will immediately stop sync operations for this node.
                              </AlertDialogDescription>
                            </AlertDialogHeader>
                            <AlertDialogFooter>
                              <AlertDialogCancel className="font-mono text-xs uppercase tracking-wider border-border/50">Cancel</AlertDialogCancel>
                              <AlertDialogAction onClick={() => handleDelete(account.id)} className="bg-destructive text-destructive-foreground hover:bg-destructive/90 font-mono text-xs uppercase tracking-wider">
                                Confirm Termination
                              </AlertDialogAction>
                            </AlertDialogFooter>
                          </AlertDialogContent>
                        </AlertDialog>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </div>
    </div>
  );
}
