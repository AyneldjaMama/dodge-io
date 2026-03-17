import type { Express } from "express";
import { createServer, type Server } from "node:http";
import { storage } from "./storage";

export async function registerRoutes(app: Express): Promise<Server> {
  app.post("/api/daily-scores", async (req, res) => {
    try {
      const { deviceId, displayName, date, score, survivalTime } = req.body;

      if (!deviceId || !date || score == null || survivalTime == null) {
        return res.status(400).json({ error: "Missing required fields" });
      }

      const name = (displayName || "Player").slice(0, 32);

      const entry = await storage.submitDailyScore({
        deviceId,
        displayName: name,
        date,
        score,
        survivalTime,
      });

      return res.json(entry);
    } catch (err) {
      console.error("Error submitting daily score:", err);
      return res.status(500).json({ error: "Failed to submit score" });
    }
  });

  app.get("/api/daily-scores/:date", async (req, res) => {
    try {
      const { date } = req.params;
      const leaderboard = await storage.getDailyLeaderboard(date, 50);
      return res.json(leaderboard);
    } catch (err) {
      console.error("Error fetching leaderboard:", err);
      return res.status(500).json({ error: "Failed to fetch leaderboard" });
    }
  });

  app.get("/api/daily-scores/:date/device/:deviceId", async (req, res) => {
    try {
      const { date, deviceId } = req.params;
      const best = await storage.getDeviceBestForDate(deviceId, date);
      return res.json(best || null);
    } catch (err) {
      console.error("Error fetching device score:", err);
      return res.status(500).json({ error: "Failed to fetch device score" });
    }
  });

  const httpServer = createServer(app);
  return httpServer;
}
