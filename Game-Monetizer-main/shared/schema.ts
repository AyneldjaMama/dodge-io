import { sql } from "drizzle-orm";
import { pgTable, text, varchar, integer, timestamp, index } from "drizzle-orm/pg-core";
import { createInsertSchema } from "drizzle-zod";
import { z } from "zod";

export const users = pgTable("users", {
  id: varchar("id")
    .primaryKey()
    .default(sql`gen_random_uuid()`),
  username: text("username").notNull().unique(),
  password: text("password").notNull(),
});

export const dailyScores = pgTable(
  "daily_scores",
  {
    id: varchar("id")
      .primaryKey()
      .default(sql`gen_random_uuid()`),
    deviceId: varchar("device_id", { length: 64 }).notNull(),
    displayName: varchar("display_name", { length: 32 }).notNull(),
    date: varchar("date", { length: 10 }).notNull(),
    score: integer("score").notNull(),
    survivalTime: integer("survival_time").notNull(),
    createdAt: timestamp("created_at").defaultNow().notNull(),
  },
  (table) => [
    index("daily_scores_date_score_idx").on(table.date, table.score),
    index("daily_scores_device_date_idx").on(table.deviceId, table.date),
  ]
);

export const insertUserSchema = createInsertSchema(users).pick({
  username: true,
  password: true,
});

export const insertDailyScoreSchema = createInsertSchema(dailyScores).pick({
  deviceId: true,
  displayName: true,
  date: true,
  score: true,
  survivalTime: true,
});

export type InsertUser = z.infer<typeof insertUserSchema>;
export type User = typeof users.$inferSelect;
export type InsertDailyScore = z.infer<typeof insertDailyScoreSchema>;
export type DailyScore = typeof dailyScores.$inferSelect;
