-- MySQL：消息 body 扩为 MEDIUMTEXT，以容纳图片/语音等 JSON 元数据。已执行过的库请勿重复执行。
ALTER TABLE im_messages MODIFY COLUMN body MEDIUMTEXT NOT NULL;
