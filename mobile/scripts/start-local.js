#!/usr/bin/env node

const fs = require("fs");
const os = require("os");
const path = require("path");
const { spawn } = require("child_process");

function findLanAddress() {
  const addresses = Object.values(os.networkInterfaces())
    .flat()
    .filter(
      (network) =>
        network &&
        network.family === "IPv4" &&
        !network.internal &&
        !network.address.startsWith("169.254.")
    )
    .map((network) => network.address);

  return (
    addresses.find((address) => address.startsWith("192.168.")) ||
    addresses.find((address) => address.startsWith("10.")) ||
    addresses.find((address) => {
      const [first, second] = address.split(".").map(Number);
      return first === 172 && second >= 16 && second <= 31;
    }) ||
    addresses[0]
  );
}

const lanAddress = findLanAddress();

if (!lanAddress) {
  console.error("No LAN IPv4 address was found. Connect this computer to Wi-Fi or Ethernet, then try again.");
  process.exit(1);
}

const apiUrl = `http://${lanAddress}:8081/api`;
const envLocalPath = path.join(__dirname, "..", ".env.local");
fs.writeFileSync(
  envLocalPath,
  [
    "# Generated automatically for local Expo development. Do not commit this file.",
    `EXPO_PUBLIC_API_URL=${apiUrl}`,
    "",
  ].join("\n")
);

console.log(`Mobile API: ${apiUrl}`);
console.log("The current LAN address has been saved to mobile/.env.local.");

const extraArguments = process.argv.slice(2);
const expo = process.platform === "win32" ? "npx.cmd" : "npx";
const child = spawn(expo, ["expo", "start", "--port", "8085", "--clear", ...extraArguments], {
  cwd: path.join(__dirname, ".."),
  stdio: "inherit",
});

child.on("exit", (code) => process.exit(code ?? 1));
