// Xitoy Do'kon — Google Apps Script
// Bu kodni Extensions → Apps Script ga joylashtiring
// Deploy → Web app → Anyone → Deploy
//
// Sheet ustunlari (0-indexed):
//  0:id  1:title  2:description  3:price  4:image  5:category
//  6:discountPercent  7:images  8:soldCount  9:active  10:inStock
//  11:discountType  12:discountExpires  13:telegramMessageId  14:originalDiscount  15:autoDelete

var SHEET_ID = "1_Gsq4ZvabXpbe5bcBOIKbL4xSiKZj80f70RYgr5LzkA";

function toBool(val, defaultVal) {
  if (val === "" || val === null || val === undefined) return defaultVal;
  return val !== 0 && val !== "0" && val !== false && val !== "false";
}

function safeParseArray(val) {
  if (!val) return [];
  try { return JSON.parse(String(val)); } catch(_) { return []; }
}

function doGet(e) {
  if (e.parameter.action === "getUserOrders" && e.parameter.telegram_id) {
    return jsonOut(getUserOrders(e.parameter.telegram_id));
  }
  if (e.parameter.action === "getAllOrders")              return jsonOut(getAllOrders());
  if (e.parameter.action === "getPendingCartReminders")  return jsonOut(getPendingCartReminders());
  if (e.parameter.action === "getSettings")              return jsonOut(getSettings());
  if (e.parameter.action === "getAllUsersWithFcmToken")  return jsonOut(getAllUsersWithFcmToken());
  return getProducts();
}

function getProducts() {
  var sheet = SpreadsheetApp.openById(SHEET_ID).getSheets()[0];
  var data  = sheet.getDataRange().getValues();

  if (data.length === 0) return jsonOut([]);

  var products = [];
  for (var i = 0; i < data.length; i++) {
    var r = data[i];
    if (!r[1]) continue;

    var imagesStr = r[7] ? String(r[7]) : "";
    products.push({
      id:                 Number(r[0]) || i,
      title:              String(r[1]),
      description:        String(r[2]),
      price:              Number(r[3]) || 0,
      image:              String(r[4]),
      category:           String(r[5]),
      discountPercent:    Number(r[6]) || 0,
      images:             imagesStr ? imagesStr.split(",").filter(Boolean) : [],
      soldCount:          Number(r[8]) || 0,
      active:             toBool(r[9],  true),
      inStock:            toBool(r[10], true),
      discountType:       String(r[11] || "doimiy"),
      discountExpires:    String(r[12] || ""),
      telegramMessageId:  Number(r[13]) || 0,
      originalDiscount:   Number(r[14]) || Number(r[6]) || 0,
      autoDelete:         toBool(r[15], false),
      rating:              Number(r[16]) || 0,
      variantlarYoqilgan:  toBool(r[17], false),
      variantNomlari:      safeParseArray(r[18]),
      variantNarxlari:     safeParseArray(r[19]).map(Number),
    });
  }
  return jsonOut(products);
}

// Foydalanuvchining buyurtmalari (telegram_id bo'yicha) — eng yangisi birinchi
// Buyurtmalar varag'i ustunlari (0-indexed):
//  0:order_id 1:telegram_id 2:fullname 3:phone 4:location_link
//  5:mahsulotlar 6:jami_summa 7:holat 8:sana
function getUserOrders(telegram_id) {
  var ss    = SpreadsheetApp.openById(SHEET_ID);
  var sheet = ss.getSheetByName("Buyurtmalar");
  if (!sheet) return { orders: [] };

  var rows = sheet.getDataRange().getValues();
  var orders = [];
  for (var i = 1; i < rows.length; i++) {
    if (String(rows[i][1]) !== String(telegram_id)) continue;
    orders.push({
      order_id:    String(rows[i][0]),
      mahsulotlar: String(rows[i][5]),
      jami_summa:  Number(rows[i][6]) || 0,
      holat:       String(rows[i][7]) || "Yangi",
      sana:        String(rows[i][8])
    });
  }
  return { orders: orders.reverse() };  // eng yangisi birinchi
}

function doPost(e) {
  try {
    var d = JSON.parse(e.postData.contents);

    if (d.action === "getUser")                return jsonOut(getUser(d.telegram_id));
    if (d.action === "saveUser")               return jsonOut(saveUser(d));
    if (d.action === "saveOrder")              return jsonOut(saveOrder(d));
    if (d.action === "saveFcmToken")           return jsonOut(saveFcmToken(d.telegram_id, d.fcm_token));
    if (d.action === "updateOrderStatus")      return jsonOut(updateOrderStatus(d.order_id, d.status));
    if (d.action === "syncCart")               return jsonOut(syncCart(d.telegram_id, d.mahsulot_nomi));
    if (d.action === "cancelCartReminder")     return jsonOut(cancelCartReminder(d.telegram_id));
    if (d.action === "updateCartReminderStage") return jsonOut(updateCartReminderStage(d.telegram_id, d.new_stage));
    if (d.action === "updateSettings")         return jsonOut(updateSettings(d.key, d.value));
    if (d.type === "updateProduct")                               return updateProduct(d);
    if (d.type === "deleteProduct" || d.type === "delete_product") return deleteProduct(d);
    if (d.type === "editProduct"   || d.type === "edit_product")   return editProduct(d);
    if (d.type === "updateMessageId")                             return updateMessageId(d);
    if (d.type === "updateDiscount")                              return updateDiscount(d);
    return saveProduct(d);

  } catch (err) {
    return jsonOut({ ok: false, error: err.toString() });
  }
}

function updateProduct(d) {
  var sheet = SpreadsheetApp.openById(SHEET_ID).getSheets()[0];
  var data  = sheet.getDataRange().getValues();

  for (var i = 0; i < data.length; i++) {
    if (Number(data[i][0]) !== Number(d.id)) continue;

    var row = i + 1;
    if (d.field === "active") {
      sheet.getRange(row, 10).setValue(d.value ? 1 : 0);
    } else if (d.field === "inStock") {
      sheet.getRange(row, 11).setValue(d.value ? 1 : 0);
    } else if (d.field === "variantlar_yoqilgan") {
      sheet.getRange(row, 18).setValue(d.value ? 1 : 0);
    } else if (d.field === "variant_nomlari") {
      sheet.getRange(row, 19).setValue(JSON.stringify(d.value || []));
    } else if (d.field === "variant_narxlari") {
      sheet.getRange(row, 20).setValue(JSON.stringify(d.value || []));
    } else {
      return jsonOut({ ok: false, error: "Noma'lum maydon: " + d.field });
    }
    return jsonOut({ ok: true });
  }

  return jsonOut({ ok: false, error: "Tovar topilmadi (id=" + d.id + ")" });
}

// telegram_message_id ni saqlaydi (tovar kanalga yuborilgandan keyin)
function updateMessageId(d) {
  var sheet = SpreadsheetApp.openById(SHEET_ID).getSheets()[0];
  var data  = sheet.getDataRange().getValues();

  for (var i = 0; i < data.length; i++) {
    if (Number(data[i][0]) !== Number(d.id)) continue;
    sheet.getRange(i + 1, 14).setValue(Number(d.message_id) || 0);
    return jsonOut({ ok: true });
  }

  return jsonOut({ ok: false, error: "Tovar topilmadi (id=" + d.id + ")" });
}

// Chegirma foizini yangilaydi (vaqt tugaganda 0 ga o'tkazish uchun)
function updateDiscount(d) {
  var sheet = SpreadsheetApp.openById(SHEET_ID).getSheets()[0];
  var data  = sheet.getDataRange().getValues();

  for (var i = 0; i < data.length; i++) {
    if (Number(data[i][0]) !== Number(d.id)) continue;
    sheet.getRange(i + 1, 7).setValue(Number(d.discount) || 0);
    return jsonOut({ ok: true });
  }

  return jsonOut({ ok: false, error: "Tovar topilmadi (id=" + d.id + ")" });
}

// Buyurtmani "Buyurtmalar" varag'iga saqlaydi.
// Ustunlar: order_id, telegram_id, fullname, phone, location_link,
//           mahsulotlar, jami_summa, holat, sana
function saveOrder(d) {
  var ss    = SpreadsheetApp.openById(SHEET_ID);
  var sheet = ss.getSheetByName("Buyurtmalar");

  if (!sheet) {
    sheet = ss.insertSheet("Buyurtmalar");
    sheet.appendRow(["order_id", "telegram_id", "fullname", "phone",
                     "location_link", "mahsulotlar", "jami_summa", "holat", "sana"]);
  }

  // order_id avtomatik: DS-{son}
  var lastRow = sheet.getLastRow();
  var orderId = "DS-" + (24800 + lastRow);

  sheet.appendRow([
    orderId,
    d.telegram_id || "",
    d.fullname || "",
    d.phone || "",
    d.location_link || "",
    d.mahsulotlar || "",
    Number(d.jami_summa) || 0,
    "Yangi",
    d.sana || ""
  ]);

  // telegram_id va telefon matn sifatida saqlansin (+ va boshlang'ich 0 yo'qolmasligi uchun)
  var newRow = sheet.getLastRow();
  sheet.getRange(newRow, 2).setNumberFormat("@");
  sheet.getRange(newRow, 4).setNumberFormat("@");

  return { success: true, order_id: orderId };
}

function saveProduct(d) {
  var sheet      = SpreadsheetApp.openById(SHEET_ID).getSheets()[0];
  var rows       = sheet.getLastRow();
  var images     = d.images || [];
  var firstImage = images.length > 0 ? images[0] : (d.image_url || "");
  var imagesStr  = images.length > 0 ? images.join(",") : (d.image_url || "");

  sheet.appendRow([
    rows,                              // 0:  id
    d.name,                            // 1:  title
    d.description,                     // 2:  description
    Number(d.price),                   // 3:  price
    firstImage,                        // 4:  image
    d.category,                        // 5:  category
    Number(d.discount) || 0,           // 6:  discountPercent
    imagesStr,                         // 7:  images
    Number(d.sold_count) || 0,         // 8:  soldCount
    1,                                 // 9:  active
    1,                                 // 10: inStock
    d.discount_type    || "doimiy",    // 11: discountType
    d.discount_expires || "",          // 12: discountExpires
    0,                                 // 13: telegramMessageId
    Number(d.discount) || 0,           // 14: originalDiscount
    d.auto_delete ? 1 : 0,            // 15: autoDelete
    Number(d.rating) || 0,            // 16: rating
    d.variantlar_yoqilgan ? 1 : 0,    // 17: variantlar_yoqilgan
    JSON.stringify(d.variant_nomlari || []),   // 18: variant_nomlari
    JSON.stringify(d.variant_narxlari || [])   // 19: variant_narxlari
  ]);

  return jsonOut({ ok: true, id: rows });
}

function deleteProduct(d) {
  var sheet = SpreadsheetApp.openById(SHEET_ID).getSheets()[0];
  var data  = sheet.getDataRange().getValues();

  for (var i = 0; i < data.length; i++) {
    if (Number(data[i][0]) !== Number(d.id)) continue;
    sheet.deleteRow(i + 1);
    return jsonOut({ ok: true });
  }

  return jsonOut({ ok: false, error: "Tovar topilmadi (id=" + d.id + ")" });
}

function editProduct(d) {
  var sheet = SpreadsheetApp.openById(SHEET_ID).getSheets()[0];
  var data  = sheet.getDataRange().getValues();

  for (var i = 0; i < data.length; i++) {
    if (Number(data[i][0]) !== Number(d.id)) continue;
    var row = i + 1;
    if (d.name)                              sheet.getRange(row, 2).setValue(d.name);
    if (d.description)                       sheet.getRange(row, 3).setValue(d.description);
    if (d.price !== undefined)               sheet.getRange(row, 4).setValue(Number(d.price));
    if (d.category)                          sheet.getRange(row, 6).setValue(d.category);
    if (d.discount !== undefined)            sheet.getRange(row, 7).setValue(Number(d.discount) || 0);
    if (d.variantlar_yoqilgan !== undefined) sheet.getRange(row, 18).setValue(d.variantlar_yoqilgan ? 1 : 0);
    if (d.variant_nomlari !== undefined)     sheet.getRange(row, 19).setValue(JSON.stringify(d.variant_nomlari || []));
    if (d.variant_narxlari !== undefined)    sheet.getRange(row, 20).setValue(JSON.stringify(d.variant_narxlari || []));
    return jsonOut({ ok: true });
  }

  return jsonOut({ ok: false, error: "Tovar topilmadi (id=" + d.id + ")" });
}

// telegram_id bo'yicha "Users" varag'idan foydalanuvchini qidiradi
// Mavjud bo'lsa { found: true, ... }, bo'lmasa { found: false }
// Users ustunlari: 0:telegram_id 1:username 2:fullname 3:phone
//                  4:latitude 5:longitude 6:location_link 7:ro_yxat_sana 8:fcm_token
function getUser(telegram_id) {
  var sheet = SpreadsheetApp.openById(SHEET_ID).getSheetByName("Users");
  if (!sheet) return { found: false };
  var rows = sheet.getDataRange().getValues();
  for (var i = 1; i < rows.length; i++) {
    if (String(rows[i][0]) === String(telegram_id)) {
      return {
        found: true,
        telegram_id:   rows[i][0],
        username:      rows[i][1],
        fullname:      rows[i][2],
        phone:         rows[i][3],
        location_link: rows[i][6],
        fcm_token:     rows[i][8] ? String(rows[i][8]) : ""
      };
    }
  }
  return { found: false };
}

// FCM token ni "Users" varag'ining 9-ustuniga (8 index) saqlaydi
function saveFcmToken(telegram_id, fcm_token) {
  var sheet = SpreadsheetApp.openById(SHEET_ID).getSheetByName("Users");
  if (!sheet) return { success: false, error: "Users varag'i topilmadi" };
  var rows = sheet.getDataRange().getValues();
  for (var i = 1; i < rows.length; i++) {
    if (String(rows[i][0]) === String(telegram_id)) {
      sheet.getRange(i + 1, 9).setValue(fcm_token);  // 9-ustun (1-indexed) = fcm_token
      return { success: true };
    }
  }
  return { success: false, error: "Foydalanuvchi topilmadi: " + telegram_id };
}

// Login orqali kirgan foydalanuvchini "Users" varag'iga saqlaydi
// Ustunlar: 0:telegram_id 1:username 2:fullname 3:phone
//           4:latitude 5:longitude 6:location_link 7:ro_yxat_sana
function saveUser(data) {
  var ss = SpreadsheetApp.openById(SHEET_ID);
  var sheet = ss.getSheetByName("Users");
  if (!sheet) {
    sheet = ss.insertSheet("Users");
    sheet.appendRow(["telegram_id", "username", "fullname", "phone",
                     "latitude", "longitude", "location_link", "ro_yxat_sana"]);
  }
  var rowValues = [
    data.telegram_id,
    data.username || "",
    data.fullname || "",
    data.phone || "",
    data.latitude || "",
    data.longitude || "",
    data.location_link || "",
    data.ro_yxat_sana || ""
  ];
  // Mavjud foydalanuvchini tekshirish — bo'lsa, ma'lumotlarni yangilash
  var rows = sheet.getDataRange().getValues();
  for (var i = 1; i < rows.length; i++) {
    if (String(rows[i][0]) === String(data.telegram_id)) {
      sheet.getRange(i + 1, 1, 1, 8).setValues([rowValues]);
      // Telefon ustuni matn sifatida saqlansin (+ va boshlang'ich 0 yo'qolmasligi uchun)
      sheet.getRange(i + 1, 4).setNumberFormat("@");
      return { success: true, updated: true };
    }
  }
  sheet.appendRow(rowValues);
  sheet.getRange(sheet.getLastRow(), 4).setNumberFormat("@");
  return { success: true, created: true };
}

// Barcha buyurtmalarni qaytaradi (admin panel uchun) — eng yangisi birinchi
function getAllOrders() {
  var ss    = SpreadsheetApp.openById(SHEET_ID);
  var sheet = ss.getSheetByName("Buyurtmalar");
  if (!sheet) return { orders: [] };

  var rows = sheet.getDataRange().getValues();
  var orders = [];
  for (var i = 1; i < rows.length; i++) {
    if (!rows[i][0]) continue;
    orders.push({
      order_id:      String(rows[i][0]),
      telegram_id:   String(rows[i][1]),
      fullname:      String(rows[i][2]),
      phone:         String(rows[i][3]),
      location_link: String(rows[i][4]),
      mahsulotlar:   String(rows[i][5]),
      jami_summa:    Number(rows[i][6]) || 0,
      holat:         String(rows[i][7]) || "Yangi",
      sana:          String(rows[i][8])
    });
  }
  return { orders: orders.reverse() };
}

// Buyurtma holatini yangilaydi (holat ustuni: 8-ustun, 1-indexed)
// Holat qiymatlari: Yangi | Tolov_kutilmoqda | Tasdiqlandi | Rad_etildi | Yo'lda | Yetkazildi
// telegram_id ni qaytaradi (FCM bildirishnoma uchun backend ishlatadi)
function updateOrderStatus(order_id, status) {
  var ss    = SpreadsheetApp.openById(SHEET_ID);
  var sheet = ss.getSheetByName("Buyurtmalar");
  if (!sheet) return { success: false, error: "Buyurtmalar varag'i topilmadi" };
  var rows = sheet.getDataRange().getValues();
  for (var i = 1; i < rows.length; i++) {
    if (String(rows[i][0]) === String(order_id)) {
      sheet.getRange(i + 1, 8).setValue(status);
      return { success: true, telegram_id: String(rows[i][1]) };
    }
  }
  return { success: false, error: "Buyurtma topilmadi: " + order_id };
}

// ── Savat eslatmaси — SavatEslatmalar varag'i ─────────────────────────────────
// Ustunlar (0-indexed): 0:telegram_id 1:mahsulot_nomi 2:qoshilgan_vaqt 3:bosqich 4:bekor_qilingan

function syncCart(telegram_id, mahsulot_nomi) {
  var ss = SpreadsheetApp.openById(SHEET_ID);
  var sheet = ss.getSheetByName("SavatEslatmalar");
  if (!sheet) {
    sheet = ss.insertSheet("SavatEslatmalar");
    sheet.appendRow(["telegram_id", "mahsulot_nomi", "qoshilgan_vaqt", "bosqich", "bekor_qilingan"]);
  }
  // Faol yozuv borligini tekshirish — bo'lsa timer davom etsin
  var rows = sheet.getDataRange().getValues();
  for (var i = 1; i < rows.length; i++) {
    var cancelled = rows[i][4];
    if (String(rows[i][0]) === String(telegram_id)
        && cancelled !== true && String(cancelled).toLowerCase() !== "true") {
      return { success: true, action: "exists" };
    }
  }
  // Yangi yozuv
  sheet.appendRow([String(telegram_id), String(mahsulot_nomi), new Date().toISOString(), 0, false]);
  return { success: true, action: "created" };
}

function cancelCartReminder(telegram_id) {
  var sheet = SpreadsheetApp.openById(SHEET_ID).getSheetByName("SavatEslatmalar");
  if (!sheet) return { success: false };
  var rows = sheet.getDataRange().getValues();
  for (var i = 1; i < rows.length; i++) {
    var cancelled = rows[i][4];
    if (String(rows[i][0]) === String(telegram_id)
        && cancelled !== true && String(cancelled).toLowerCase() !== "true") {
      sheet.getRange(i + 1, 5).setValue(true);
    }
  }
  return { success: true };
}

function getPendingCartReminders() {
  var sheet = SpreadsheetApp.openById(SHEET_ID).getSheetByName("SavatEslatmalar");
  if (!sheet) return { reminders: [] };
  var rows = sheet.getDataRange().getValues();
  var reminders = [];
  for (var i = 1; i < rows.length; i++) {
    if (!rows[i][0]) continue;
    var cancelled = rows[i][4];
    if (cancelled === true || String(cancelled).toLowerCase() === "true") continue;
    reminders.push({
      telegram_id:    String(rows[i][0]),
      mahsulot_nomi:  String(rows[i][1]),
      qoshilgan_vaqt: String(rows[i][2]),
      bosqich:        Number(rows[i][3]) || 0
    });
  }
  return { reminders: reminders };
}

function updateCartReminderStage(telegram_id, new_stage) {
  var sheet = SpreadsheetApp.openById(SHEET_ID).getSheetByName("SavatEslatmalar");
  if (!sheet) return { success: false };
  var rows = sheet.getDataRange().getValues();
  for (var i = 1; i < rows.length; i++) {
    var cancelled = rows[i][4];
    if (String(rows[i][0]) === String(telegram_id)
        && cancelled !== true && String(cancelled).toLowerCase() !== "true") {
      sheet.getRange(i + 1, 4).setValue(Number(new_stage));
      return { success: true };
    }
  }
  return { success: false };
}

// ── Sozlamalar — Settings varag'i ─────────────────────────────────────────────
// Ustunlar: 0:key 1:value

function getSettings() {
  var ss = SpreadsheetApp.openById(SHEET_ID);
  var sheet = ss.getSheetByName("Settings");
  if (!sheet) return { marketing_notifications_enabled: true };
  var rows = sheet.getDataRange().getValues();
  var result = { marketing_notifications_enabled: true };
  for (var i = 1; i < rows.length; i++) {
    if (String(rows[i][0]) === "marketing_notifications_enabled") {
      result.marketing_notifications_enabled =
        (rows[i][1] === true || String(rows[i][1]).toLowerCase() === "true");
    }
  }
  return result;
}

function updateSettings(key, value) {
  var ss = SpreadsheetApp.openById(SHEET_ID);
  var sheet = ss.getSheetByName("Settings");
  if (!sheet) {
    sheet = ss.insertSheet("Settings");
    sheet.appendRow(["key", "value"]);
  }
  var rows = sheet.getDataRange().getValues();
  for (var i = 1; i < rows.length; i++) {
    if (String(rows[i][0]) === String(key)) {
      sheet.getRange(i + 1, 2).setValue(value);
      return { success: true };
    }
  }
  sheet.appendRow([String(key), value]);
  return { success: true };
}

// ── FCM token bor barcha foydalanuvchilar ────────────────────────────────────

function getAllUsersWithFcmToken() {
  var sheet = SpreadsheetApp.openById(SHEET_ID).getSheetByName("Users");
  if (!sheet) return { users: [] };
  var rows = sheet.getDataRange().getValues();
  var users = [];
  for (var i = 1; i < rows.length; i++) {
    var fcm = rows[i][8] ? String(rows[i][8]) : "";
    if (!fcm) continue;
    users.push({
      telegram_id: String(rows[i][0]),
      fullname:    String(rows[i][2] || ""),
      fcm_token:   fcm
    });
  }
  return { users: users };
}

function jsonOut(data) {
  return ContentService
    .createTextOutput(JSON.stringify(data))
    .setMimeType(ContentService.MimeType.JSON);
} 