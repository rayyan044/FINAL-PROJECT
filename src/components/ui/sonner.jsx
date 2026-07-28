import { Toaster as Sonner } from "sonner";
const Toaster = ({ ...props }) => {
  return (
    <Sonner
      className="toaster group"
      position="top-right"
      closeButton
      richColors
      duration={4000}
      toastOptions={{
        classNames: {
          toast: "fef-toast",
          success: "fef-toast-success",
          error: "fef-toast-error",
        },
      }}
      {...props}
    />
  );
};
export { Toaster };
