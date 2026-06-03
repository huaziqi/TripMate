export interface WeatherData {
  city: string         // 城市名，如 "北京市"
  weather: string      // 天气状况，如 "晴"、"多云"
  temperature: string  // 温度，如 "25"
  winddirection: string // 风向，如 "东北"
  windpower: string    // 风力等级，如 "3"
  humidity: string     // 湿度，如 "65"
  reporttime: string   // 数据更新时间，如 "2026-05-26 14:00:00"
}
