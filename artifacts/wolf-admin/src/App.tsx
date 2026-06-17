import { useEffect, useRef } from "react";
import { Switch, Route, Router as WouterRouter, useLocation, Redirect } from "wouter";
import { QueryClient, QueryClientProvider, useQueryClient } from "@tanstack/react-query";
import { ClerkProvider, SignIn, SignUp, Show, useClerk } from "@clerk/react";
import { publishableKeyFromHost } from "@clerk/react/internal";
import { shadcn } from "@clerk/themes";
import { Toaster } from "@/components/ui/toaster";
import { TooltipProvider } from "@/components/ui/tooltip";
import NotFound from "@/pages/not-found";
import Dashboard from "@/pages/dashboard";
import AccountsList from "@/pages/accounts/list";
import AccountDetail from "@/pages/accounts/detail";
import { Shell } from "@/components/layout/shell";

const queryClient = new QueryClient();

const clerkPubKey = publishableKeyFromHost(
  window.location.hostname,
  import.meta.env.VITE_CLERK_PUBLISHABLE_KEY,
);

const clerkProxyUrl = import.meta.env.VITE_CLERK_PROXY_URL;

const basePath = import.meta.env.BASE_URL.replace(/\/$/, "");

function stripBase(path: string): string {
  return basePath && path.startsWith(basePath)
    ? path.slice(basePath.length) || "/"
    : path;
}

if (!clerkPubKey) {
  throw new Error("Missing VITE_CLERK_PUBLISHABLE_KEY");
}

const clerkAppearance = {
  theme: shadcn,
  cssLayerName: "clerk",
  options: {
    logoPlacement: "inside" as const,
    logoLinkUrl: basePath || "/",
    logoImageUrl: `${window.location.origin}${basePath}/logo.svg`,
  },
  variables: {
    colorPrimary: "#08b4e0",
    colorForeground: "#f8fafc",
    colorMutedForeground: "#8ba3bf",
    colorDanger: "#e53e3e",
    colorBackground: "#0f1a2e",
    colorInput: "#1e2d42",
    colorInputForeground: "#f8fafc",
    colorNeutral: "#1e2d42",
    fontFamily: "Inter, sans-serif",
    borderRadius: "0.375rem",
  },
  elements: {
    rootBox: "w-full flex justify-center",
    cardBox: "bg-[#0f1a2e] border border-[#1e2d42] rounded-xl w-[440px] max-w-full overflow-hidden shadow-2xl",
    card: "!shadow-none !border-0 !bg-transparent !rounded-none",
    footer: "!shadow-none !border-0 !bg-transparent !rounded-none",
    headerTitle: "text-[#f8fafc] font-bold",
    headerSubtitle: "text-[#8ba3bf]",
    socialButtonsBlockButtonText: "text-[#f8fafc]",
    formFieldLabel: "text-[#8ba3bf]",
    footerActionLink: "text-[#08b4e0]",
    footerActionText: "text-[#8ba3bf]",
    dividerText: "text-[#8ba3bf]",
    identityPreviewEditButton: "text-[#08b4e0]",
    formFieldSuccessText: "text-[#38a169]",
    alertText: "text-[#f8fafc]",
    logoBox: "flex justify-center pt-2",
    logoImage: "h-10 w-auto",
    socialButtonsBlockButton: "border-[#1e2d42] bg-[#1a2a3e] hover:bg-[#223347] text-[#f8fafc]",
    formButtonPrimary: "bg-[#08b4e0] hover:bg-[#06a0c9] text-white",
    formFieldInput: "bg-[#1e2d42] border-[#2a3a50] text-[#f8fafc] placeholder:text-[#4a6080]",
    footerAction: "border-t border-[#1e2d42]",
    dividerLine: "bg-[#1e2d42]",
    alert: "bg-[#1e2d42] border-[#2a3a50]",
    otpCodeFieldInput: "bg-[#1e2d42] border-[#2a3a50] text-[#f8fafc]",
    formFieldRow: "",
    main: "",
  },
};

function SignInPage() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-background px-4">
      <SignIn routing="path" path={`${basePath}/sign-in`} signUpUrl={`${basePath}/sign-up`} />
    </div>
  );
}

function SignUpPage() {
  return (
    <div className="flex min-h-screen items-center justify-center bg-background px-4">
      <SignUp routing="path" path={`${basePath}/sign-up`} signInUrl={`${basePath}/sign-in`} />
    </div>
  );
}

function ClerkQueryClientCacheInvalidator() {
  const { addListener } = useClerk();
  const qc = useQueryClient();
  const prevUserIdRef = useRef<string | null | undefined>(undefined);

  useEffect(() => {
    const unsubscribe = addListener(({ user }) => {
      const userId = user?.id ?? null;
      if (prevUserIdRef.current !== undefined && prevUserIdRef.current !== userId) {
        qc.clear();
      }
      prevUserIdRef.current = userId;
    });
    return unsubscribe;
  }, [addListener, qc]);

  return null;
}

function HomeRedirect() {
  return (
    <>
      <Show when="signed-in">
        <Redirect to="/dashboard" />
      </Show>
      <Show when="signed-out">
        <div className="flex min-h-screen items-center justify-center bg-background px-4">
          <div className="flex flex-col items-center gap-6 text-center max-w-sm">
            <img src={`${basePath}/logo.svg`} alt="Wolf NOC" className="h-12 w-auto" />
            <p className="text-muted-foreground text-sm">
              Panel de administración para Wolf IPTV.<br />
              Inicia sesión para continuar.
            </p>
            <a
              href={`${basePath}/sign-in`}
              className="inline-flex items-center justify-center rounded-md bg-primary px-6 py-2 text-sm font-medium text-white hover:bg-primary/90 transition-colors"
            >
              Iniciar sesión
            </a>
          </div>
        </div>
      </Show>
    </>
  );
}

function ProtectedRoute({ component: Component }: { component: React.ComponentType }) {
  return (
    <>
      <Show when="signed-in">
        <Shell>
          <Component />
        </Shell>
      </Show>
      <Show when="signed-out">
        <Redirect to="/sign-in" />
      </Show>
    </>
  );
}

function Router() {
  return (
    <Switch>
      <Route path="/" component={HomeRedirect} />
      <Route path="/dashboard" component={() => <ProtectedRoute component={Dashboard} />} />
      <Route path="/accounts" component={() => <ProtectedRoute component={AccountsList} />} />
      <Route path="/accounts/:id" component={() => <ProtectedRoute component={AccountDetail} />} />
      <Route path="/sign-in/*?" component={SignInPage} />
      <Route path="/sign-up/*?" component={SignUpPage} />
      <Route component={NotFound} />
    </Switch>
  );
}

function ClerkProviderWithRoutes() {
  const [, setLocation] = useLocation();

  return (
    <ClerkProvider
      publishableKey={clerkPubKey}
      proxyUrl={clerkProxyUrl}
      appearance={clerkAppearance}
      signInUrl={`${basePath}/sign-in`}
      signUpUrl={`${basePath}/sign-up`}
      localization={{
        signIn: {
          start: {
            title: "Wolf NOC — Admin",
            subtitle: "Inicia sesión para gestionar tus cuentas IPTV",
          },
        },
        signUp: {
          start: {
            title: "Crear cuenta",
            subtitle: "Acceso al panel de administración Wolf IPTV",
          },
        },
      }}
      routerPush={(to) => setLocation(stripBase(to))}
      routerReplace={(to) => setLocation(stripBase(to), { replace: true })}
    >
      <QueryClientProvider client={queryClient}>
        <ClerkQueryClientCacheInvalidator />
        <TooltipProvider>
          <Router />
          <Toaster />
        </TooltipProvider>
      </QueryClientProvider>
    </ClerkProvider>
  );
}

function App() {
  return (
    <WouterRouter base={basePath}>
      <ClerkProviderWithRoutes />
    </WouterRouter>
  );
}

export default App;
