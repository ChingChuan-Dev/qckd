# QCKD

一个 Minecraft 服务器插件，用于统计玩家的KD数据。

## 版本信息

- **适配版本**: Minecraft 1.21.8
- **插件版本**: 1.0.0
- **作者**: Jason_Arad, _M1dori

## 功能特性

- 实时统计玩家击杀数和死亡数
- 自动计算 K/D 比率
- 支持开启/关闭统计功能
- 可配置插件启用状态
- 热重载配置文件

## 指令说明

| 指令 | 权限 | 说明 |
|------|------|------|
| `/qckd` | 无 | 查看帮助信息和版本 |
| `/qckd help` | 无 | 查看帮助信息 |
| `/qckd statistics start` | `qckd.admin` | 开始统计 K/D 数据 |
| `/qckd statistics stop` | `qckd.admin` | 停止统计 K/D 数据 |
| `/qckd reload` | `qckd.admin` | 重载插件配置 |

## 权限节点

- `qckd.admin` - 管理员权限（控制统计开关和重载配置）
## 许可协议

本插件采用 [CC BY-NC-ND 4.0](https://creativecommons.org/licenses/by-nc-nd/4.0/deed.zh-hans/) 协议发布。  
您可以自由转载和分享，但必须保留署名，且不得用于商业目的或分发修改后的版本。
