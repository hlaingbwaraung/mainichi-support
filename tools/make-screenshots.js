const fs = require("fs");
const path = require("path");

const outDir = path.join(process.cwd(), "screenshots");
fs.mkdirSync(outDir, { recursive: true });

const W = 390;
const H = 844;
const bg = "#faf8f3";
const cardBg = "#fffffc";
const text = "#1d1e20";
const muted = "#605d56";
const primary = "#222426";
const secondary = "#5c5850";
const accent = "#b08939";
const line = "#e0d7c3";
const font = "sans-serif";

function esc(value) {
  return String(value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function wrapText(value, maxChars) {
  const lines = [];
  for (let i = 0; i < value.length; i += maxChars) {
    lines.push(value.slice(i, i + maxChars));
  }
  return lines;
}

class Screen {
  constructor(title, subtitle) {
    this.y = 46;
    this.parts = [
      `<svg xmlns="http://www.w3.org/2000/svg" width="${W}" height="${H}" viewBox="0 0 ${W} ${H}">`,
      `<rect width="${W}" height="${H}" fill="${bg}"/>`,
      `<path d="M0 42H${W}M0 84H${W}M0 126H${W}M0 168H${W}M0 210H${W}M0 252H${W}M0 294H${W}M0 336H${W}M0 378H${W}M0 420H${W}M0 462H${W}M0 504H${W}M0 546H${W}M0 588H${W}M0 630H${W}M0 672H${W}M0 714H${W}M0 756H${W}M0 798H${W}" stroke="${accent}" stroke-opacity="0.10"/>`,
      `<path d="M42 0V${H}M84 0V${H}M126 0V${H}M168 0V${H}M210 0V${H}M252 0V${H}M294 0V${H}M336 0V${H}M378 0V${H}" stroke="${primary}" stroke-opacity="0.08"/>`,
      `<rect x="20" y="22" width="4" height="34" fill="${accent}"/>`,
      `<text x="36" y="${this.y}" font-family="${font}" font-size="31" font-weight="700" fill="${text}">${esc(title)}</text>`,
    ];
    this.y += 38;
    this.parts.push(`<text x="20" y="${this.y}" font-family="${font}" font-size="20" fill="${muted}">${esc(subtitle)}</text>`);
    this.y += 42;
  }

  label(value) {
    this.parts.push(`<text x="20" y="${this.y + 24}" font-family="${font}" font-size="22" font-weight="700" fill="${text}">${esc(value)}</text>`);
    this.y += 42;
  }

  body(value, bold = false, size = 20) {
    const lines = wrapText(value, 18);
    for (const line of lines) {
      this.parts.push(`<text x="20" y="${this.y + size}" font-family="${font}" font-size="${size}" ${bold ? 'font-weight="700"' : ""} fill="${text}">${esc(line)}</text>`);
      this.y += size + 10;
    }
    this.y += 4;
  }

  button(value, color = primary, height = 78, size = 24) {
    this.parts.push(`<rect x="20" y="${this.y}" width="350" height="${height}" rx="6" fill="${color}" stroke="${accent}" stroke-width="1"/>`);
    this.parts.push(`<text x="195" y="${this.y + height / 2 + 8}" text-anchor="middle" font-family="${font}" font-size="${size}" font-weight="700" fill="#fff">${esc(value)}</text>`);
    this.y += height + 14;
  }

  smallButton(value) {
    this.button(value, secondary, 56, 20);
  }

  field(value, height = 58) {
    this.parts.push(`<rect x="20" y="${this.y}" width="350" height="${height}" rx="6" fill="#fff" stroke="${line}" stroke-width="1"/>`);
    this.parts.push(`<text x="36" y="${this.y + 36}" font-family="${font}" font-size="22" fill="${muted}">${esc(value)}</text>`);
    this.y += height + 14;
  }

  card(lines, action = "削除") {
    const cardHeight = 34 + lines.length * 34 + (action ? 72 : 18);
    this.parts.push(`<rect x="20" y="${this.y}" width="350" height="${cardHeight}" rx="6" fill="${cardBg}" stroke="${line}" stroke-width="1"/>`);
    let localY = this.y + 36;
    for (const item of lines) {
      this.parts.push(`<text x="36" y="${localY}" font-family="${font}" font-size="${item.size || 22}" ${item.bold ? 'font-weight="700"' : ""} fill="${text}">${esc(item.text)}</text>`);
      localY += item.size && item.size > 22 ? 42 : 34;
    }
    if (action) {
      this.parts.push(`<rect x="36" y="${this.y + cardHeight - 58}" width="318" height="44" rx="6" fill="${secondary}"/>`);
      this.parts.push(`<text x="195" y="${this.y + cardHeight - 30}" text-anchor="middle" font-family="${font}" font-size="20" font-weight="700" fill="#fff">${esc(action)}</text>`);
    }
    this.y += cardHeight + 14;
  }

  timeControl(value) {
    this.parts.push(`<rect x="20" y="${this.y}" width="350" height="260" rx="6" fill="#fff" stroke="${line}" stroke-width="1"/>`);
    this.parts.push(`<text x="195" y="${this.y + 62}" text-anchor="middle" font-family="${font}" font-size="40" font-weight="700" fill="${text}">${esc(value)}</text>`);
    const buttons = [
      ["時", "-1", "+1", 108],
      ["分", "-1", "+1", 186],
    ];
    for (const [label, minus, plus, y] of buttons) {
      this.parts.push(`<text x="60" y="${this.y + y + 28}" text-anchor="middle" font-family="${font}" font-size="24" font-weight="700" fill="${text}">${label}</text>`);
      this.parts.push(`<rect x="105" y="${this.y + y}" width="110" height="58" rx="6" fill="${secondary}"/>`);
      this.parts.push(`<text x="160" y="${this.y + y + 37}" text-anchor="middle" font-family="${font}" font-size="24" font-weight="700" fill="#fff">${minus}</text>`);
      this.parts.push(`<rect x="235" y="${this.y + y}" width="110" height="58" rx="6" fill="${secondary}"/>`);
      this.parts.push(`<text x="290" y="${this.y + y + 37}" text-anchor="middle" font-family="${font}" font-size="24" font-weight="700" fill="#fff">${plus}</text>`);
    }
    this.y += 274;
  }

  graph() {
    const values = [2100, 2800, 1900, 3600, 3100, 4300, 3284];
    const labels = ["月", "火", "水", "木", "金", "土", "日"];
    this.card([{ text: "今日の歩数", bold: true }, { text: "3,284 歩", bold: true, size: 34 }, { text: "7日間の歩数", bold: true }], null);
    const y = this.y - 150;
    const x = 36;
    const max = Math.max(...values);
    const gap = 8;
    const bw = (318 - gap * 6) / 7;
    this.parts.push(`<line x1="${x}" y1="${y + 100}" x2="${x + 318}" y2="${y + 100}" stroke="${line}"/>`);
    values.forEach((value, i) => {
      const h = Math.max(10, 90 * value / max);
      const bx = x + i * (bw + gap);
      this.parts.push(`<rect x="${bx}" y="${y + 100 - h}" width="${bw}" height="${h}" rx="5" fill="${i === 6 ? accent : primary}"/>`);
      this.parts.push(`<text x="${bx + bw / 2}" y="${y + 126}" text-anchor="middle" font-family="${font}" font-size="14" fill="${muted}">${labels[i]}</text>`);
    });
  }

  save(name) {
    this.parts.push("</svg>");
    fs.writeFileSync(path.join(outDir, `${name}.svg`), this.parts.join("\n"));
  }
}

function home() {
  const s = new Screen("まいにちサポート", "今日");
  s.label("使いたい機能を選んでください");
  ["歩数計", "メモ", "予定", "薬アラーム", "緊急連絡", "今日やること", "買い物リスト"].forEach((label) => s.button(label));
  s.button("プレミアム会員", accent);
  s.save("home-luxury");
}

function steps() {
  const s = new Screen("歩数計", "今日の歩数を大きく表示");
  s.smallButton("戻る");
  s.graph();
  s.save("steps-luxury");
}

function schedule() {
  const s = new Screen("予定", "予定の時間に音でお知らせ");
  s.smallButton("戻る");
  s.button("予定を追加");
  s.card([{ text: "病院", bold: true }, { text: "2026/05/28 10:30" }]);
  s.card([{ text: "家族へ電話", bold: true }, { text: "2026/05/28 18:00" }]);
  s.save("schedule-luxury");
}

function scheduleForm() {
  const s = new Screen("予定を追加", "日付と時間を選んで保存");
  s.smallButton("予定へ戻る");
  s.label("予定の名前");
  s.field("例：病院");
  s.label("時間");
  s.timeControl("09:35");
  s.button("保存");
  s.save("schedule-form-luxury");
}

function medicine() {
  const s = new Screen("薬アラーム", "薬の時間を音でお知らせ");
  s.smallButton("戻る");
  s.button("薬を追加");
  s.card([{ text: "薬: 朝の薬", bold: true }, { text: "2026/05/29 08:00" }]);
  s.save("medicine-luxury");
}

function emergency() {
  const s = new Screen("緊急連絡", "家族や病院へすぐ電話");
  s.smallButton("戻る");
  s.card([{ text: "登録番号", bold: true }, { text: "090-1234-5678", size: 28 }], null);
  s.button("電話する");
  s.smallButton("番号を設定");
  s.save("emergency-luxury");
}

function todos() {
  const s = new Screen("今日やること", "終わったら完了");
  s.smallButton("戻る");
  s.button("やることを追加");
  s.card([{ text: "薬を飲む", bold: true }], "完了");
  s.card([{ text: "散歩する", bold: true }], "完了");
  s.save("todos-luxury");
}

function shopping() {
  const s = new Screen("買い物リスト", "買う物を大きく表示");
  s.smallButton("戻る");
  s.button("買う物を追加");
  s.card([{ text: "牛乳", bold: true }], "買った");
  s.card([{ text: "卵", bold: true }], "買った");
  s.save("shopping-luxury");
}

home();
steps();
schedule();
scheduleForm();
medicine();
emergency();
todos();
shopping();
