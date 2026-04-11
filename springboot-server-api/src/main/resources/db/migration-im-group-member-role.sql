-- MySQL：为已有库增加群成员角色（群主 / 管理员 / 成员）。已执行过的库请勿重复执行。
ALTER TABLE im_group_members
    ADD COLUMN role VARCHAR(16) NOT NULL DEFAULT 'MEMBER';

UPDATE im_group_members m
    INNER JOIN im_groups g ON m.group_id = g.id AND m.user_id = g.owner_user_id
    SET m.role = 'OWNER'
    WHERE m.role = 'MEMBER';
