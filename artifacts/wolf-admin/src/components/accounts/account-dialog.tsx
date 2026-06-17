import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { useCreateAccount, useUpdateAccount, getListAccountsQueryKey, getGetStatsQueryKey, getGetAccountQueryKey } from "@workspace/api-client-react";
import { useQueryClient } from "@tanstack/react-query";
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle, DialogTrigger, DialogFooter } from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form";
import { Input } from "@/components/ui/input";
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group";
import { Switch } from "@/components/ui/switch";
import { useToast } from "@/hooks/use-toast";
import type { Account, AccountInputType } from "@workspace/api-client-react/src/generated/api.schemas";

const formSchema = z.object({
  type: z.enum(["xtream", "m3u"] as const),
  name: z.string().min(1, "Name is required"),
  host: z.string().optional(),
  user: z.string().optional(),
  pass: z.string().optional(),
  url: z.string().optional(),
  active: z.boolean().default(true),
}).superRefine((data, ctx) => {
  if (data.type === "xtream") {
    if (!data.host) ctx.addIssue({ code: z.ZodIssueCode.custom, message: "Host is required for Xtream", path: ["host"] });
    if (!data.user) ctx.addIssue({ code: z.ZodIssueCode.custom, message: "Username is required for Xtream", path: ["user"] });
    if (!data.pass) ctx.addIssue({ code: z.ZodIssueCode.custom, message: "Password is required for Xtream", path: ["pass"] });
  } else if (data.type === "m3u") {
    if (!data.url) ctx.addIssue({ code: z.ZodIssueCode.custom, message: "URL is required for M3U", path: ["url"] });
  }
});

type FormValues = z.infer<typeof formSchema>;

interface AccountDialogProps {
  children: React.ReactNode;
  accountToEdit?: Account;
}

export function AccountDialog({ children, accountToEdit }: AccountDialogProps) {
  const [open, setOpen] = useState(false);
  const { toast } = useToast();
  const queryClient = useQueryClient();
  const isEditing = !!accountToEdit;

  const createAccount = useCreateAccount();
  const updateAccount = useUpdateAccount();

  const form = useForm<FormValues>({
    resolver: zodResolver(formSchema),
    defaultValues: {
      type: accountToEdit?.type || "xtream",
      name: accountToEdit?.name || "",
      host: accountToEdit?.host || "",
      user: accountToEdit?.user || "",
      pass: accountToEdit?.pass || "",
      url: accountToEdit?.url || "",
      active: accountToEdit?.active ?? true,
    },
  });

  const protocolType = form.watch("type");

  const onSubmit = (values: FormValues) => {
    if (isEditing) {
      updateAccount.mutate(
        { id: accountToEdit.id, data: values },
        {
          onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: getListAccountsQueryKey() });
            queryClient.invalidateQueries({ queryKey: getGetAccountQueryKey(accountToEdit.id) });
            queryClient.invalidateQueries({ queryKey: getGetStatsQueryKey() });
            setOpen(false);
            toast({ title: "Node Updated", description: "Configuration saved successfully." });
          },
        }
      );
    } else {
      createAccount.mutate(
        { data: values },
        {
          onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: getListAccountsQueryKey() });
            queryClient.invalidateQueries({ queryKey: getGetStatsQueryKey() });
            setOpen(false);
            form.reset();
            toast({ title: "Node Provisioned", description: "New configuration added to network." });
          },
        }
      );
    }
  };

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        {children}
      </DialogTrigger>
      <DialogContent className="sm:max-w-[500px] bg-card border-border/50">
        <DialogHeader>
          <DialogTitle className="font-mono uppercase tracking-wider text-primary">
            {isEditing ? "Modify Configuration" : "Provision Node"}
          </DialogTitle>
          <DialogDescription className="font-mono text-xs">
            {isEditing ? "Update existing endpoint parameters." : "Define parameters for a new provider endpoint."}
          </DialogDescription>
        </DialogHeader>

        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6 mt-4">
            <FormField
              control={form.control}
              name="type"
              render={({ field }) => (
                <FormItem className="space-y-3">
                  <FormLabel className="text-[10px] font-mono uppercase text-muted-foreground tracking-widest">Protocol Type</FormLabel>
                  <FormControl>
                    <RadioGroup
                      onValueChange={field.onChange}
                      defaultValue={field.value}
                      className="grid grid-cols-2 gap-4"
                      disabled={isEditing}
                    >
                      <FormItem>
                        <FormControl>
                          <RadioGroupItem value="xtream" className="peer sr-only" />
                        </FormControl>
                        <FormLabel className="flex flex-col items-center justify-between rounded-md border-2 border-muted bg-transparent p-4 hover:bg-accent hover:text-accent-foreground peer-data-[state=checked]:border-primary peer-data-[state=checked]:bg-primary/5 cursor-pointer">
                          <span className="font-mono font-bold tracking-wider">XTREAM</span>
                        </FormLabel>
                      </FormItem>
                      <FormItem>
                        <FormControl>
                          <RadioGroupItem value="m3u" className="peer sr-only" />
                        </FormControl>
                        <FormLabel className="flex flex-col items-center justify-between rounded-md border-2 border-muted bg-transparent p-4 hover:bg-accent hover:text-accent-foreground peer-data-[state=checked]:border-primary peer-data-[state=checked]:bg-primary/5 cursor-pointer">
                          <span className="font-mono font-bold tracking-wider">M3U</span>
                        </FormLabel>
                      </FormItem>
                    </RadioGroup>
                  </FormControl>
                </FormItem>
              )}
            />

            <div className="space-y-4 bg-black/20 p-4 rounded-lg border border-border/30">
              <FormField
                control={form.control}
                name="name"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel className="text-[10px] font-mono uppercase text-muted-foreground tracking-widest">Alias / Identifier</FormLabel>
                    <FormControl>
                      <Input placeholder="e.g. Primary US Node" className="font-mono text-sm bg-background/50" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              {protocolType === "xtream" && (
                <>
                  <FormField
                    control={form.control}
                    name="host"
                    render={({ field }) => (
                      <FormItem>
                        <FormLabel className="text-[10px] font-mono uppercase text-muted-foreground tracking-widest">Endpoint URL</FormLabel>
                        <FormControl>
                          <Input placeholder="http://portal.provider.com:8080" className="font-mono text-sm bg-background/50" {...field} />
                        </FormControl>
                        <FormMessage />
                      </FormItem>
                    )}
                  />
                  <div className="grid grid-cols-2 gap-4">
                    <FormField
                      control={form.control}
                      name="user"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel className="text-[10px] font-mono uppercase text-muted-foreground tracking-widest">Username</FormLabel>
                          <FormControl>
                            <Input placeholder="username" className="font-mono text-sm bg-background/50" {...field} />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                    <FormField
                      control={form.control}
                      name="pass"
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel className="text-[10px] font-mono uppercase text-muted-foreground tracking-widest">Password</FormLabel>
                          <FormControl>
                            <Input type="password" placeholder="••••••••" className="font-mono text-sm bg-background/50" {...field} />
                          </FormControl>
                          <FormMessage />
                        </FormItem>
                      )}
                    />
                  </div>
                </>
              )}

              {protocolType === "m3u" && (
                <FormField
                  control={form.control}
                  name="url"
                  render={({ field }) => (
                    <FormItem>
                      <FormLabel className="text-[10px] font-mono uppercase text-muted-foreground tracking-widest">Payload URL</FormLabel>
                      <FormControl>
                        <Input placeholder="http://provider.com/get.php?username=..." className="font-mono text-sm bg-background/50" {...field} />
                      </FormControl>
                      <FormMessage />
                    </FormItem>
                  )}
                />
              )}
            </div>

            <FormField
              control={form.control}
              name="active"
              render={({ field }) => (
                <FormItem className="flex flex-row items-center justify-between rounded-lg border border-border/30 p-4 bg-black/20">
                  <div className="space-y-0.5">
                    <FormLabel className="text-sm font-mono uppercase tracking-wider">Initial State</FormLabel>
                    <DialogDescription className="font-mono text-[10px]">
                      Enable syncing for this node immediately.
                    </DialogDescription>
                  </div>
                  <FormControl>
                    <Switch
                      checked={field.value}
                      onCheckedChange={field.onChange}
                      className="data-[state=checked]:bg-emerald-500"
                    />
                  </FormControl>
                </FormItem>
              )}
            />

            <DialogFooter>
              <Button type="submit" className="w-full font-mono uppercase tracking-wider text-xs" disabled={createAccount.isPending || updateAccount.isPending}>
                {isEditing ? "Commit Changes" : "Provision Node"}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  );
}
