# 移除 Claude 共同作者记录处理流程

处理时间：2026-06-29  
仓库：`https://github.com/cocochick-hub/Warehouse-Management-System.git`  
本地仓库目录：`D:\Warehouse-Management-System\Warehouse-Management-System`

## 当前情况

这次问题已经处理完成。

远端 `dev` 和 `main` 都已经重写到新历史：

```text
origin/dev  = d2d028ae82685876161bccc56cbea8d5d8e8e192
origin/main = 5c7d99bba767a73a77ef1e16df3427ea2b78b85a
```

这两个远端分支里已经查不到：

```text
Co-Authored-By: Claude
```

新旧分支的文件内容 diff 为空，意思是：这次只改了提交信息，没有改任何业务代码、配置文件或文档内容。

本地还保留两个备份分支：

```text
backup/remove-claude-dev-20260629-164155
backup/remove-claude-main-20260629-164155
```

这两个备份分支指向修复前的旧历史，用来兜底回滚。它们只在本地存在，不影响 GitHub 当前 `main` / `dev` 的贡献者统计。

当前工作区还有未提交改动：

```text
M  backend/src/main/resources/application.yml
?? docs/git-remove-claude-coauthor-20260629.md
```

其中 `application.yml` 是处理前就存在的本地改动；本文档是本次新增的处理记录。

## 问题原因

`dev` 分支的部分提交信息里包含下面这种 Git trailer：

```text
Co-Authored-By: Claude <noreply@anthropic.com>
Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>
```

GitHub 会把 `Co-Authored-By` 识别为共同作者。因此这些提交合并进 `main` 后，Claude 会被 GitHub 统计为贡献者。

要解决这个问题，不能只新增一个修复提交，因为旧提交里的共同作者记录仍然存在。正确做法是重写包含问题的提交信息，删除 Claude 的 `Co-Authored-By` 行，然后把 `dev` 和已经合并过的 `main` 都推送到新历史。

## 处理原则

1. 只删除 commit message 里的 Claude 共同作者 trailer。
2. 不改文件内容。
3. 同时处理 `dev` 和 `main`，因为 `main` 已经合并了 `dev`。
4. 推送时使用 `--force-with-lease`，避免覆盖别人刚刚推送的新提交。
5. 处理前创建本地备份分支，保留旧历史以便回滚。

## 操作记录

### 1. 检查当前目录是不是 Git 仓库

这一步的目的：先确认当前命令执行目录是不是仓库根目录。如果不是仓库，后续 Git 命令会读不到历史。

输入：

```powershell
git status --short --branch
git log --oneline --decorate --graph --format="%h %d %an <%ae> | %cn <%ce> | %s" -n 20
```

结果：

```text
fatal: not a git repository (or any of the parent directories): .git
```

结果含义：当前目录 `D:\Warehouse-Management-System` 不是 Git 仓库。需要继续找真正包含 `.git` 的目录。

### 2. 定位实际 Git 仓库

这一步的目的：在当前目录下面查找 `.git` 文件夹，找到真正的仓库根目录。

输入：

```powershell
Get-ChildItem -Force
Get-ChildItem -Force -Directory -Recurse -Depth 3 |
  Where-Object { $_.Name -eq '.git' } |
  Select-Object -ExpandProperty FullName
```

结果：

```text
D:\Warehouse-Management-System\Warehouse-Management-System\.git
```

结果含义：实际仓库目录是：

```text
D:\Warehouse-Management-System\Warehouse-Management-System
```

后续所有 Git 命令都在这个目录下执行。

### 3. 检查本地分支状态和最近提交

这一步的目的：确认当前分支、是否有未提交改动，以及最近提交里是否有 Claude 共同作者记录。

输入：

```powershell
git status --short --branch
git log --oneline --decorate --graph --format="%h %d %an <%ae> | %cn <%ce> | %s" -n 30
git log --format="%H%nAuthor: %an <%ae>%nCommitter: %cn <%ce>%n%B%n---END---" -n 8
```

关键结果：

```text
## dev...origin/dev
 M backend/src/main/resources/application.yml
```

结果含义：当前在 `dev` 分支，本地 `dev` 与 `origin/dev` 对齐；同时本地已有一个未提交改动：`backend/src/main/resources/application.yml`。后续处理需要避免覆盖这个文件。

还发现 `dev` 上有 4 个提交包含 Claude trailer：

```text
7658258 ... Co-Authored-By: Claude <noreply@anthropic.com>
bd32eb2 ... Co-Authored-By: Claude <noreply@anthropic.com>
0776ee3 ... Co-Authored-By: Claude <noreply@anthropic.com>
5a0bae8 ... Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>
```

结果含义：问题源头在 `dev` 历史里，不是单纯的 `main` merge commit。

### 4. 拉取远端最新状态

这一步的目的：确认 GitHub 上最新的 `main` 和 `dev` 状态，避免基于过期信息处理历史。

输入：

```powershell
git fetch origin --prune
```

结果：

```text
From https://github.com/cocochick-hub/Warehouse-Management-System
   a055f61..ea85ec8  main       -> origin/main
```

结果含义：远端 `main` 刚刚从 `a055f61` 更新到了 `ea85ec8`。这说明你说的“刚刚把 dev 合并到 main”已经发生在 GitHub 上了。

### 5. 确认 `origin/main` 是否包含问题提交

这一步的目的：确认 `main` 是不是已经包含 `dev` 的问题提交。如果包含，就必须同时修 `main` 和 `dev`。

输入：

```powershell
git log --oneline --decorate --graph --format="%h %d %an <%ae> | %cn <%ce> | %s" -n 20 origin/main
git log origin/main --format="%h %H %d %an <%ae> %s%n%B%n---END---" --grep="Co-Authored-By: Claude"
git merge-base origin/main origin/dev
```

关键结果：

```text
*   ea85ec8 (origin/main, origin/HEAD) ... Merge pull request #1 from cocochick-hub/dev
|\  
| * ac96c99 (HEAD -> dev, origin/dev) ... 完善转包功能与封存可用量逻辑
| * 5a0bae8 ... feat: 阶段三盘点模块 + 阶段二测试 + 转包Bug修复
| * 0776ee3 ... feat: 操作日志审计 + 报表Excel导出 + 路由权限修复 + 全局异常兜底
| * bd32eb2 ... feat(mobile): 实现转包功能 + 阶段一全量修复
| * 7658258 ... feat(mobile): 实现移动端封存/解封功能 + 项目结构调整
```

`merge-base` 结果：

```text
ac96c9988c1e992ee3cc7ec00008cb86e96e6e13
```

结果含义：`origin/main` 的最新提交是一个 PR merge，已经把 `origin/dev` 合进去了。所以如果只修 `dev`，`main` 里仍然会保留旧的 Claude 共同作者记录；必须同时修两个分支。

### 6. 创建本地备份分支

这一步的目的：重写历史前保留旧分支指针。如果后续需要回滚，可以通过这些备份分支找到修复前的状态。

输入：

```powershell
$ts = Get-Date -Format 'yyyyMMdd-HHmmss'
$mainBackup = "backup/remove-claude-main-$ts"
$devBackup = "backup/remove-claude-dev-$ts"
git branch $mainBackup origin/main
git branch $devBackup origin/dev
"Created $mainBackup and $devBackup"
```

结果：

```text
branch 'backup/remove-claude-main-20260629-164155' set up to track 'origin/main'.
branch 'backup/remove-claude-dev-20260629-164155' set up to track 'origin/dev'.
Created backup/remove-claude-main-20260629-164155 and backup/remove-claude-dev-20260629-164155
```

结果含义：本地已经保留了修复前的 `main` 和 `dev`。这一步不会推送到 GitHub，也不会影响远端。

### 7. 创建临时镜像仓库

这一步的目的：在临时 bare/mirror 仓库里重写历史，避免直接在你的工作区里操作，降低误伤未提交文件的风险。

输入：

```powershell
$tmp = Join-Path $env:TEMP ("wms-remove-claude-" + (Get-Date -Format 'yyyyMMddHHmmss'))
git clone --mirror . $tmp
$tmp
```

结果：

```text
C:\Users\ZhuanZ\AppData\Local\Temp\wms-remove-claude-20260629164208
Cloning into bare repository 'C:\Users\ZhuanZ\AppData\Local\Temp\wms-remove-claude-20260629164208'...
done.
```

结果含义：临时镜像仓库创建成功。后续历史重写先在这里完成，不会触碰工作区文件。

### 8. 对齐临时镜像里的 `dev` 和 `main`

这一步的目的：确保临时仓库里的 `main` 指向远端最新 `origin/main`，而不是本地旧 `main`。

输入：

```powershell
git update-ref refs/heads/main refs/remotes/origin/main
git rev-parse refs/heads/dev refs/heads/main
git remote -v
```

结果：

```text
ac96c9988c1e992ee3cc7ec00008cb86e96e6e13
ea85ec8e9458f22ce97332d466ce0f8d591b66d7
origin  D:/Warehouse-Management-System/Warehouse-Management-System/. (fetch)
origin  D:/Warehouse-Management-System/Warehouse-Management-System/. (push)
```

结果含义：临时仓库中待处理的旧指针是：

```text
dev  = ac96c9988c1e992ee3cc7ec00008cb86e96e6e13
main = ea85ec8e9458f22ce97332d466ce0f8d591b66d7
```

这些 SHA 后面会用于 `--force-with-lease`，确保推送时远端还停留在预期旧版本。

### 9. 重写提交信息，删除 Claude trailer

这一步的目的：遍历 `dev` 和 `main` 的历史提交信息，删除所有以 `Co-Authored-By: Claude` 开头的行。

输入：

```powershell
git filter-branch --force --msg-filter "sed '/^Co-Authored-By: Claude/d'" -- refs/heads/dev refs/heads/main
```

关键结果：

```text
Ref 'refs/heads/dev' was rewritten
Ref 'refs/heads/main' was rewritten
```

结果含义：`dev` 和 `main` 的提交历史已经在临时仓库中被重写。因为 Git 的提交 SHA 包含提交信息，所以即使文件内容不变，相关提交的 SHA 也会变化。

### 10. 验证重写结果

这一步的目的：确认两个关键点：第一，Claude trailer 已经不存在；第二，文件内容没有变化。

输入：

```powershell
git log refs/heads/dev refs/heads/main --format="%h %H %s%n%B%n---END---" --grep="Co-Authored-By: Claude"
git rev-parse refs/heads/dev refs/heads/main refs/original/refs/heads/dev refs/original/refs/heads/main
git diff --stat refs/original/refs/heads/dev refs/heads/dev
git diff --stat refs/original/refs/heads/main refs/heads/main
```

结果：

```text
# grep 无输出，表示新 dev/main 中没有 Claude trailer

d2d028ae82685876161bccc56cbea8d5d8e8e192
5c7d99bba767a73a77ef1e16df3427ea2b78b85a
ac96c9988c1e992ee3cc7ec00008cb86e96e6e13
ea85ec8e9458f22ce97332d466ce0f8d591b66d7

# 两次 git diff --stat 均无输出
```

结果含义：

```text
dev:  ac96c9988c1e992ee3cc7ec00008cb86e96e6e13 -> d2d028ae82685876161bccc56cbea8d5d8e8e192
main: ea85ec8e9458f22ce97332d466ce0f8d591b66d7 -> 5c7d99bba767a73a77ef1e16df3427ea2b78b85a
```

`git log --grep` 没有输出，说明新历史里找不到 Claude 共同作者记录。`git diff --stat` 没有输出，说明新旧分支的文件树一致，也就是没有改代码，只改了提交信息。

### 11. 第一次推送尝试

这一步的目的：把临时仓库里的新 `dev` 和 `main` 推送到 GitHub，并用 `--force-with-lease` 防止误覆盖别人新增的远端提交。

输入：

```powershell
git remote set-url origin https://github.com/cocochick-hub/Warehouse-Management-System.git
git push origin refs/heads/dev:refs/heads/dev --force-with-lease=refs/heads/dev:ac96c9988c1e992ee3cc7ec00008cb86e96e6e13
git push origin refs/heads/main:refs/heads/main --force-with-lease=refs/heads/main:ea85ec8e9458f22ce97332d466ce0f8d591b66d7
```

结果：

```text
fatal: --mirror can't be combined with refspecs
fatal: --mirror can't be combined with refspecs
```

结果含义：临时仓库是用 `git clone --mirror` 创建的，remote 配置里带有 mirror 模式，不能和指定 refspec 一起推送。这次推送失败，没有任何内容被推到 GitHub。

### 12. 关闭临时 remote 的 mirror 配置并重新推送

这一步的目的：取消临时仓库 remote 的 mirror 模式，然后重新执行带 lease 的指定分支推送。

输入：

```powershell
git config remote.origin.mirror false
git push origin refs/heads/dev:refs/heads/dev --force-with-lease=refs/heads/dev:ac96c9988c1e992ee3cc7ec00008cb86e96e6e13
git push origin refs/heads/main:refs/heads/main --force-with-lease=refs/heads/main:ea85ec8e9458f22ce97332d466ce0f8d591b66d7
```

结果：

```text
To https://github.com/cocochick-hub/Warehouse-Management-System.git
 + ac96c99...d2d028a dev -> dev (forced update)
To https://github.com/cocochick-hub/Warehouse-Management-System.git
 + ea85ec8...5c7d99b main -> main (forced update)
```

结果含义：GitHub 上的 `dev` 和 `main` 已经成功更新到新历史。`forced update` 是预期结果，因为重写提交信息会改变提交 SHA，需要强制更新分支指针。

### 13. 同步本地分支

这一步的目的：把本地仓库的 `origin/dev`、`origin/main` 和本地分支指针同步到刚推送的新历史，同时保留工作区未提交改动。

输入：

```powershell
git fetch origin --prune
git reset --soft origin/dev
git branch -f main origin/main
git status --short --branch
```

结果：

```text
From https://github.com/cocochick-hub/Warehouse-Management-System
 + ac96c99...d2d028a dev        -> origin/dev  (forced update)
 + ea85ec8...5c7d99b main       -> origin/main  (forced update)
branch 'main' set up to track 'origin/main'.
## dev...origin/dev
 M backend/src/main/resources/application.yml
```

结果含义：本地 `dev` 已经对齐 `origin/dev`，本地 `main` 已经对齐 `origin/main`。`application.yml` 的未提交改动仍然保留，没有被覆盖。

### 14. 最终验证

这一步的目的：在本地重新检查远端跟踪分支，确认修复结果已经落到 `origin/main` 和 `origin/dev`。

输入：

```powershell
git log origin/main origin/dev --format="%h %H %s%n%B%n---END---" --grep="Co-Authored-By: Claude"
git diff --stat backup/remove-claude-dev-20260629-164155 origin/dev
git diff --stat backup/remove-claude-main-20260629-164155 origin/main
```

结果：

```text
# grep 无输出
# 两次 git diff --stat 均无输出
```

结果含义：`origin/main` 和 `origin/dev` 中已经没有 Claude 共同作者记录。新旧分支文件内容一致，说明这次处理没有引入代码变化。

## 其他协作者需要怎么处理

如果其他协作者已经拉过旧的 `dev` 或 `main`，他们本地会和远端新历史分叉。需要重新同步。

如果本地没有未提交改动，可以执行：

```powershell
git fetch origin --prune
git switch dev
git reset --hard origin/dev
git switch main
git reset --hard origin/main
```

这些命令的含义是：重新拉取远端引用，然后把本地 `dev` 和 `main` 强制对齐到 GitHub 上的新历史。

如果本地有未提交改动，不要直接 `reset --hard`。先保存改动：

```powershell
git stash
git fetch origin --prune
git switch dev
git reset --hard origin/dev
git switch main
git reset --hard origin/main
git stash pop
```

如果 `stash pop` 出现冲突，需要按普通 Git 冲突处理。

## 注意事项

GitHub 的贡献者统计可能有缓存，不一定立刻刷新。提交历史已经修掉后，通常等待一段时间或刷新页面即可看到变化。

以后如果使用 AI 生成提交信息，需要检查最后几行是否出现 `Co-Authored-By: Claude...`。如果不希望 GitHub 统计 AI 为共同作者，提交前删掉这些行。
