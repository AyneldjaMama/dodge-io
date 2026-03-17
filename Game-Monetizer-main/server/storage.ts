import { type User, type InsertUser, type InsertDailyScore, type DailyScore, dailyScores } from "@shared/schema";
import { randomUUID } from "crypto";
import { desc, eq, and } from "drizzle-orm";
import { drizzle } from "drizzle-orm/node-postgres";
import * as schema from "@shared/schema";

export interface IStorage {
  getUser(id: string): Promise<User | undefined>;
  getUserByUsername(username: string): Promise<User | undefined>;
  createUser(user: InsertUser): Promise<User>;
  submitDailyScore(entry: InsertDailyScore): Promise<DailyScore>;
  getDailyLeaderboard(date: string, limit?: number): Promise<DailyScore[]>;
  getDeviceBestForDate(deviceId: string, date: string): Promise<DailyScore | undefined>;
}

export class MemStorage implements IStorage {
  private users: Map<string, User>;

  constructor() {
    this.users = new Map();
  }

  async getUser(id: string): Promise<User | undefined> {
    return this.users.get(id);
  }

  async getUserByUsername(username: string): Promise<User | undefined> {
    return Array.from(this.users.values()).find(
      (user) => user.username === username,
    );
  }

  async createUser(insertUser: InsertUser): Promise<User> {
    const id = randomUUID();
    const user: User = { ...insertUser, id };
    this.users.set(id, user);
    return user;
  }

  async submitDailyScore(_entry: InsertDailyScore): Promise<DailyScore> {
    throw new Error("MemStorage does not support daily scores");
  }

  async getDailyLeaderboard(_date: string): Promise<DailyScore[]> {
    return [];
  }

  async getDeviceBestForDate(_deviceId: string, _date: string): Promise<DailyScore | undefined> {
    return undefined;
  }
}

export class DatabaseStorage implements IStorage {
  private db;

  constructor() {
    this.db = drizzle(process.env.DATABASE_URL!, { schema });
  }

  async getUser(id: string): Promise<User | undefined> {
    const [user] = await this.db
      .select()
      .from(schema.users)
      .where(eq(schema.users.id, id));
    return user;
  }

  async getUserByUsername(username: string): Promise<User | undefined> {
    const [user] = await this.db
      .select()
      .from(schema.users)
      .where(eq(schema.users.username, username));
    return user;
  }

  async createUser(insertUser: InsertUser): Promise<User> {
    const [user] = await this.db.insert(schema.users).values(insertUser).returning();
    return user;
  }

  async submitDailyScore(entry: InsertDailyScore): Promise<DailyScore> {
    const existing = await this.getDeviceBestForDate(entry.deviceId, entry.date);
    if (existing && existing.score >= entry.score) {
      return existing;
    }
    if (existing) {
      const [updated] = await this.db
        .update(dailyScores)
        .set({
          score: entry.score,
          survivalTime: entry.survivalTime,
          displayName: entry.displayName,
        })
        .where(eq(dailyScores.id, existing.id))
        .returning();
      return updated;
    }
    const [score] = await this.db.insert(dailyScores).values(entry).returning();
    return score;
  }

  async getDailyLeaderboard(date: string, limit = 50): Promise<DailyScore[]> {
    return this.db
      .select()
      .from(dailyScores)
      .where(eq(dailyScores.date, date))
      .orderBy(desc(dailyScores.score))
      .limit(limit);
  }

  async getDeviceBestForDate(deviceId: string, date: string): Promise<DailyScore | undefined> {
    const [best] = await this.db
      .select()
      .from(dailyScores)
      .where(and(eq(dailyScores.deviceId, deviceId), eq(dailyScores.date, date)))
      .orderBy(desc(dailyScores.score))
      .limit(1);
    return best;
  }
}

export const storage = process.env.DATABASE_URL
  ? new DatabaseStorage()
  : new MemStorage();
