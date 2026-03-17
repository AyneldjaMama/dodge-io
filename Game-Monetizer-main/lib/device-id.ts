import AsyncStorage from "@react-native-async-storage/async-storage";

const DEVICE_ID_KEY = "dodgeio-device-id";
const DISPLAY_NAME_KEY = "dodgeio-display-name";

let cachedDeviceId: string | null = null;

function generateId(): string {
  return Date.now().toString(36) + Math.random().toString(36).substr(2, 9);
}

function generateDefaultName(): string {
  const num = Math.floor(Math.random() * 9999)
    .toString()
    .padStart(4, "0");
  return `Player_${num}`;
}

export async function getDeviceId(): Promise<string> {
  if (cachedDeviceId) return cachedDeviceId;

  let id = await AsyncStorage.getItem(DEVICE_ID_KEY);
  if (!id) {
    id = generateId();
    await AsyncStorage.setItem(DEVICE_ID_KEY, id);
  }
  cachedDeviceId = id;
  return id;
}

export async function getDisplayName(): Promise<string> {
  const name = await AsyncStorage.getItem(DISPLAY_NAME_KEY);
  if (name) return name;
  const defaultName = generateDefaultName();
  await AsyncStorage.setItem(DISPLAY_NAME_KEY, defaultName);
  return defaultName;
}

export async function setDisplayName(name: string): Promise<void> {
  const trimmed = name.trim().slice(0, 32);
  if (trimmed.length === 0) return;
  await AsyncStorage.setItem(DISPLAY_NAME_KEY, trimmed);
}
