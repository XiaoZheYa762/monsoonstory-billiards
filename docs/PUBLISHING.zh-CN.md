# 上传 GitHub 与模组发布流程

适用项目：**季风物语：桌球 / Monsoon Story: Billiards**；版本：**1.1.1**。
更新日期：**2026-10-30**。官方源码仓库为 [XiaoZheYa762/monsoonstory-billiards](https://github.com/XiaoZheYa762/monsoonstory-billiards)，安装包在 [Releases](https://github.com/XiaoZheYa762/monsoonstory-billiards/releases)。平台审核状态与 GitHub 发布独立。

## 1. 先区分三个位置

| 位置 | 用途 | 应放内容 |
|---|---|---|
| GitHub 仓库的 Code 页面 | 查看源码、开发、提 Issue | 完整可构建源码、资源、构建脚本和文档，展开成文件目录 |
| GitHub Releases | 玩家和开发者下载指定版本 | 四个安装 JAR、完整源码 ZIP、清单、更新说明 |
| CurseForge / Modrinth 等模组平台 | 玩家发现项目、按游戏版本安装 | 分平台的安装 JAR、介绍、图片、依赖和许可信息 |

不要只把源码 ZIP 当成仓库唯一文件。应先解压，再把其中的目录和文件提交到 Code 页面；ZIP 本身作为 Release 附件。

## 2. 当前许可应如何写

| 内容 | 文件与许可 | 实际规则 |
|---|---|---|
| 原创代码、脚本、文档 | `LICENSE`：Jeefeng Source License v1.0 | 个人非商业使用；分发修改版必须提供完整对应源码、沿用同一协议、保留署名并标明修改；商业使用或售卖需书面授权 |
| 原创贴图、模型、GUI、图标等 | `ASSET-LICENSE.md`：Jeefeng Story Asset License | 保留权利；允许个人非商业游戏使用和本地构建运行；复用或重新分发须另行书面授权 |
| 第三方材料 | `THIRD_PARTY_NOTICES.md`、`gradle/` 内声明 | 继续遵守原协议，CC0 音效不会变成专有素材 |
| 版权及变更 | `NOTICE` | 记录版权人、新旧授权边界 |
| 外部贡献 | `CONTRIBUTING.md` | 贡献者保留版权；提交代码适用本项目协议，资产需明确授权 |

平台许可证字段选择 **Custom / 自定义**，名称填写 **Jeefeng Source License v1.0**，URL 指向真实仓库中 `LICENSE` 文件的网页。另在介绍中链接 `ASSET-LICENSE.md`。若平台只提供受限的选项，使用其“自定义/其他”入口或联系平台支持，不要为了填表而选 MIT、GPL 或把整个项目标为 All Rights Reserved。GitHub 不能识别自定义协议时显示“Other”是正常的；以文件正文为准。

可使用以下简介：

> 源码采用 Jeefeng Source License v1.0（源码可见、非商业、分发修改版须提供同协议完整源码）；原创美术及创意素材单独授权，保留权利。商业使用或售卖、素材复用及重新分发须取得权利人书面许可。第三方组件遵守各自协议。

这不是 OSI 标准开源许可证，也不是 GPL 增加商业禁令。按当前严格素材条款，**把原创素材随免费整合包或修改版重新打包也需要许可**。单纯链接官方页面不等于重新分发文件。若以后想允许免费整合包，需要在素材许可中明确增加范围，不能只在平台介绍里笼统写“可随意整合”。

GitHub 的服务条款另行允许用户查看和在 GitHub 内 Fork 公开仓库，本许可不试图取消权利人已经通过平台条款授予的权利。仓库内 Fork 不等于可以提取素材另用，或把含素材的模组对外重新打包售卖。

新方案从 1.1.1 随包提供。若旧副本已经按 MIT 对外发布，更换当前文件不能撤回旧副本已经授予的权限。保留旧 Release 和授权记录，不覆盖旧版本附件。采用自定义协议还须确认自己持有相关权利；具体争议和正式商业授权可交由知识产权律师审阅。

## 3. 上传哪些目录和文件

四个平台共用一个仓库，不必各建一个。

| 必须提交 | 原因 |
|---|---|
| `common/` | 共享 Java 源码和正式游戏资源 |
| `fabric/`、`forge/`、`neoforge/`、`neoforge-26.1/` | 四个平台的源码、元数据、验证源码和独立 Gradle 工程 |
| 各平台的 `gradlew`、`gradlew.bat`、`gradle/wrapper/` | 别人按指定 Gradle 版本构建；`gradle-wrapper.jar` 必须提交 |
| 根目录 `gradle/` | 资源转换、授权打包脚本和 Gradle 第三方声明 |
| `art/` | 可编辑 Blockbench 模型和有 CC0 授权的音效源文件；公开文件不代表放弃素材版权 |
| `tools/`、`table_geometry.json` | 生成资源、验证和打包所需工具与配置 |
| `docs/`、`.github/` | 发布说明、真实截图、验证记录、CI 和 Issue 模板 |
| `LICENSE`、`ASSET-LICENSE.md`、`ASSETS_LICENSE.md`、`NOTICE`、`THIRD_PARTY_NOTICES.md`、`CONTRIBUTING.md` | 完整授权及贡献说明；旧复数资产许可文件仅保留跳转说明 |
| `README.md`、`README.zh-CN.md`、`CHANGELOG.md`、`AI_USAGE.md`、`.gitignore`、`.gitattributes` | 项目介绍、历史、披露及版本管理配置 |

以下不提交到 Code 页面：

- `build/`、`dist/`、`.gradle/`、`.tools/`、`out/`；下载的 JDK、Gradle 缓存、反编译依赖和安装器。
- `run/`、`runs/`、世界存档、日志、崩溃报告、内存转储、截图测试的临时环境。
- `.idea/`、`.vscode/`、`__pycache__/`、本地配置、`.env`、密钥、Token、账号数据。
- 服务器玩家名单、`server.properties`、`ops.json` 等运行资料。
- 管理员上传的歌曲、音乐库和客户端下载缓存。
- Minecraft 游戏本体或其他模组的 JAR。唯一按源码跟踪的 JAR 是 Gradle Wrapper。

这些路径已由 `.gitignore` 和源码打包脚本排除。发行安装 JAR 放 Releases，不能因为 `build/` 被忽略就认为没有安装包。

## 4. 推荐：用 GitHub Desktop 首次上传

1. 安装 [GitHub Desktop](https://desktop.github.com/)，登录你希望作为作者的账号。
2. 可以直接使用本工程；也可以把 `dist/1.1.1/monsoonstory_billiards-1.1.1-github-source.zip` 解压到一个干净文件夹，例如 `E:\GitHub\monsoonstory-billiards`。确认该目录直接包含 `common/`、`fabric/`、`LICENSE` 等，不要多套一层。
3. 在 Desktop 中选择 **File → Add local repository**，指定这个目录。若提示不是仓库，点 **create a repository here**。创建时核对最终目录，避免在已有工程内再创建一个空的同名子目录。
4. 如果需要填写 **Name**，用目录本身的名字；**Local path** 选其父目录，使最终路径仍指向刚才的源码目录。仓库名建议 `monsoonstory-billiards`。不自动添加 License 或 Git ignore，不覆盖现有文件。
5. 查看 **Changes**。应看到上一节列出的源码和资料；不应看到 `build`、`run`、`.tools`、私人音乐等。特别确认四个平台的 `gradle/wrapper/gradle-wrapper.jar` 都在。
6. Summary 填 `Prepare 1.1.1 source-available release`，点击 **Commit to main**。如果 Desktop 初始化时已自动提交部分文件，仍要提交剩余项目文件。
7. 点击 **Publish repository**，Name 填 `monsoonstory-billiards`，需要公开源码时取消 **Keep this code private**，选择正确的个人账号或组织，点击发布。
8. 网页确认 Code 页面展开显示完整源码、README 和 `LICENSE`，并检查 **Actions** 的 Build。四个平台是四个独立构建任务，失败时打开具体日志排查，不能把本地成功当作线上 CI 已成功。
9. 确认仓库 **Settings → General → Features → Issues** 已启用。README 中的授权申请渠道使用该 Issues 页面。

如果已有 Git 仓库或远端，先检查当前历史和地址，正常提交、推送更新；不要重建历史、删除远端或强制覆盖。

## 5. 也可以用 Git 命令上传

下面适用于**还没有 Git 仓库**的源码目录。命令中的账号和邮箱须替换为自己的；邮箱可以使用 GitHub 提供的 noreply 邮箱。

先在 GitHub 网页右上角 **+ → New repository** 创建公开仓库，名字 `monsoonstory-billiards`。不要勾选初始化 README、`.gitignore` 或 License，保持远端为空。

在源码根目录打开 PowerShell，逐步执行：

```powershell
git init -b main
git config user.name "你的 GitHub 用户名"
git config user.email "你的 Git 提交邮箱"
git add .
git status --short
git diff --cached --stat
```

检查暂存清单符合第 3 节后再提交、推送：

```powershell
git commit -m "Prepare 1.1.1 source-available release"
git remote add origin https://github.com/YOUR_GITHUB_USERNAME/monsoonstory-billiards.git
git remote -v
git push -u origin main
```

把 `YOUR_GITHUB_USERNAME` 换成真实账号。使用 Git Credential Manager 的浏览器登录或自己配置的 SSH；不要把 Token 写在仓库 URL、脚本或源码中。如果已有 `.git`，跳过 `git init`；若已有 `origin`，先用 `git remote -v` 核对地址，不重复添加。远端已包含文件时应先正常同步，不能直接 force push。

## 6. 建库后补真实项目链接

本工程已为正式仓库补入以下真实地址。以后迁移仓库时需同步更新这些字段：

- Fabric：在 `fabric/src/main/resources/fabric.mod.json` 中添加 `contact.sources` 和 `contact.issues`。
- Forge：在主 `mods.toml` 的顶层添加 `issueTrackerURL`，在 `[[mods]]` 区块添加 `displayURL`。
- 两个 NeoForge：在各自主 `neoforge.mods.toml` 中添加对应 `issueTrackerURL` 和 `displayURL`。
- 平台项目的 Source 链接填仓库首页，Issues 链接填仓库首页加 `/issues`，License 链接指向 `LICENSE` 文件网页。

改完要提交，并重新构建、验证、打包；最终 Release 必须对应这些文件所在的提交。

## 7. 构建、检查并生成本地发布包

使用 JDK 21 运行前三个平台的 Gradle；Gradle 按需获取 Java 17 工具链。NeoForge 26.1 的 Gradle 使用 JDK 25。Python 工具需要 **Python 3.11 或更高版本**（只用标准库）。

在源码根目录执行，先确认 `java -version` 为 21：

```powershell
.\fabric\gradlew.bat -p fabric clean build
.\forge\gradlew.bat -p forge clean build
.\neoforge\gradlew.bat -p neoforge clean build
```

为当前 PowerShell 配置自己的 JDK 25 安装路径后，再构建 26.1：

```powershell
$env:JAVA_HOME = '你的 JDK 25 安装目录'
.\neoforge-26.1\gradlew.bat -p neoforge-26.1 clean build
```

不要照抄占位安装路径。除四个构建成功外，首次正式发行还应按 `docs/VERIFICATION.md` 对最终安装 JAR 进行实际客户端、服务端及所宣称的多人玩法验证。本次授权变更的检查范围另见 `docs/VERIFICATION-1.1.1.md`；旧报告不能作为新文件已实测的证明。

构建成功后：

```powershell
python tools/verify_public.py
python tools/package_release.py
```

打包脚本会先检查四个安装包中的版本、资源、Java 字节码和完整授权文件，再生成：

```text
dist/1.1.1/
  monsoonstory_billiards-fabric-1.20.1-1.1.1.jar
  monsoonstory_billiards-forge-1.20.1-1.1.1.jar
  monsoonstory_billiards-neoforge-1.21.1-1.1.1.jar
  monsoonstory_billiards-neoforge-26.1-1.1.1.jar
  monsoonstory_billiards-1.1.1-github-source.zip
  SOURCE_MANIFEST.json
  SHA256SUMS.txt
```

`SOURCE_MANIFEST.json` 记录源码 ZIP 内每个文件的 SHA-256，`SHA256SUMS.txt` 记录安装包、源码 ZIP 和源码清单的摘要。`-sources.jar` 只是 Gradle 的源码构件，不替代包含所有工程和工具的完整源码 ZIP。`-verification.jar`、`-dev.jar` 都不是玩家安装文件。

## 8. 创建 GitHub Release

1. 提交并 Push 与安装包对应的全部源码；检查线上 Actions，确认 Tag 将指向正确提交。
2. 打开仓库 **Releases → Draft a new release**。
3. **Choose a tag** 输入 `v1.1.1` 并创建，Target 选包含本次源码的分支或提交；不要复用、移动旧版本 Tag。
4. 标题填写 `Monsoon Story: Billiards 1.1.1`。说明写本次授权变更、四个平台支持范围、依赖、安装方法和验证范围，可参考 `CHANGELOG.md`。
5. 拖入 `dist/1.1.1/` 中上述 **7 个文件**。附件全部传完后可先 **Save draft** 复核，再 **Publish release**。
6. 若只准备发布 Fabric，也可以只附 Fabric JAR 与源码、清单；其他平台须等实际准备好后再发布。不要把其他 Loader 的文件标成 Fabric。
7. 正式版才设置 Latest；实际是公开测试版时使用真实的 Beta 版本号并同步修改四个 `mod_version` 后重新构建，再选 Pre-release，不能只改网页标签。
8. 退出账号或用无痕窗口检查公开可见性和附件下载；下载所需 JAR，用 `Get-FileHash -Algorithm SHA256 '文件路径'` 对照清单。

GitHub 自动附加的 Source code ZIP/TAR 是 Tag 的源码快照，不能代替玩家安装 JAR。四个 JAR 也不能一起装入同一个实例；按游戏版本和 Loader 选一个，并在客户端、服务器两边安装对应版本。

## 9. 发布到模组平台

先建立项目，再按平台/游戏版本分别上传文件：

| 安装 JAR | Loader / Minecraft | 玩家 Java | 必需依赖 |
|---|---|---|---|
| `...fabric-1.20.1-1.1.1.jar` | Fabric / 1.20.1 | 17+ | Fabric Loader 0.16.9+、Fabric API（1.20.1 版本） |
| `...forge-1.20.1-1.1.1.jar` | Forge / 1.20.1 | 17+ | Forge 47.4.28+ |
| `...neoforge-1.21.1-1.1.1.jar` | NeoForge / 1.21.1 | 21+ | NeoForge 21.1.264+ |
| `...neoforge-26.1-1.1.1.jar` | NeoForge / 精确 26.1 | 25+ | NeoForge 26.1.0.19-beta 起、低于 26.1.1 |

项目名使用 **Monsoon Story: Billiards** / **季风物语：桌球**。英文介绍参考 README，中文参考 README.zh-CN；写清八球、九球、多人球桌、摆设和管理员上传 OGG 音乐。客户端和服务器均须安装。Better Combat 是可选兼容，不要列为必需依赖；测试范围见对应版本的验证记录。

填写项目介绍、可用的分类、Source/Issues/License 链接、环境、依赖、每份文件的 Loader、Minecraft 精确版本、Release/Beta 类型和更新说明。不要额外勾选未验证的 26.1.1、26.1.2 或其他版本。按平台当时要求准备手工制作且权利明确的图标，画廊采用真实游戏截图；历史截图须注明对应版本。本次没有新增 1.1.1 游戏截图。

**Modrinth 有实际资格限制。** 截至 2026-10-30，官方要求显著 AI 生成内容如实披露，禁止在项目页面使用主要或完全 AI 生成的图片，并禁止把完全或几乎完全依赖 AI、人的参与仅限提示或测试的项目公开发布。本工程 `AI_USAGE.md` 明确写有“大量代码来自 AI。”；这不单独确定项目比例，但提交前必须依真实开发过程判断并向平台确认资格，不能以“勾选披露就一定能上架”理解。若属于禁止范围，不应提交公开发布或隐瞒来源以规避。GitHub 源码公开与 Modrinth 审核是不同事项。

CurseForge 也需按当前审核规则填写版权、项目描述和文件资料。审核要求修改时，在原项目按意见修正，不反复建新项目规避。分发平台依据你接受的平台条款托管作者提交的文件，不代表普通用户自动获得商业销售或资产复用权限；确认平台条款与自己的授权安排一致。

公开源码和发行包不保存发布 Token、不含自动上架工作流、不绑定收款账号。GitHub 发布使用本机已有登录；模组平台账号操作及收益计划由作者按自身决定进行。

## 10. 后续每次更新

1. 修改源码；四个 `gradle.properties` 的 `mod_version` 保持一致，更新中英文 README 版本、CHANGELOG 和当前验证记录。
2. 构建、按改动完成必要验证，执行打包脚本。
3. 提交并 Push；核对 CI 和 Release 要绑定的提交。
4. 建新 Tag / Release，上传本次目录里的文件；不覆盖旧包、不混用旧源码和新 JAR。
5. 在模组平台为相应 Loader 新建版本，填写本次说明和准确兼容范围。

## 官方参考

核对日期：2026-10-30。

- [OSI：开源定义](https://opensource.org/osd)
- [GitHub：本地源码上传](https://docs.github.com/en/migrations/importing-source-code/using-the-command-line-to-import-source-code/adding-locally-hosted-code-to-github)
- [GitHub Desktop：发布本地仓库](https://docs.github.com/en/desktop/adding-and-cloning-repositories/adding-an-existing-project-to-github-using-github-desktop)
- [GitHub：管理 Release](https://docs.github.com/en/repositories/releasing-projects-on-github/managing-releases-in-a-repository)
- [GitHub 服务条款：用户内容及公开仓库的 Fork 权利](https://docs.github.com/en/site-policy/github-terms/github-terms-of-service#d-user-generated-content)
- [Modrinth 内容规则](https://modrinth.com/legal/rules)
- [Modrinth 内容披露与 AI 使用](https://support.modrinth.com/en/articles/16551575-disclosure-and-usage-of-ai)
- [CurseForge 审核规则](https://support.curseforge.com/support/solutions/articles/9000197279-moderation-policies)
