[![Support my work](https://img.shields.io/badge/Support_my_work-F16061?style=for-the-badge&logo=ko-fi&logoColor=white)](https://ko-fi.com/S2X12424XK)
[![Modrinth](https://img.shields.io/badge/Modrinth-00AF5C?style=for-the-badge&logo=modrinth&logoColor=white)](https://modrinth.com/mod/fair-experience)
[![CurseForge](https://img.shields.io/badge/CurseForge-F44336?style=for-the-badge&logo=curseforge&logoColor=white)](https://www.curseforge.com/minecraft/mc-mods/fair-experience)

# Fair Experience

**Fair Experience** is a Minecraft 1.12.2 Forge mod that converts flat XP level costs into **fair raw XP costs** for Enchanting Tables, Anvils, and level-deducting mechanics.

In vanilla Minecraft, higher levels require exponentially more raw XP per level. If you are Level 46 and use a Level 30 enchantment slot, vanilla subtracts a flat 3 levels (taking you down to Level 43), costing you over **900+ raw XP**. If you were Level 30, those same 3 levels would only cost **306 raw XP**.

**Fair Experience fixes this imbalance completely.**

---

## 🌟 Key Features

- **Fair Enchanting**: Subtracts the exact raw XP value of the enchantment requirement (e.g. 30 $\rightarrow$ 27) instead of flat level subtractions.
- **Fair Anvils**: Converts anvil level costs (e.g. 12 levels) to the raw XP equivalent of those levels, saving high-level players from massive XP penalties.
- **Universal Mod Compatibility**: Works seamlessly with modded enchanting tables (e.g., *Apotheosis* with 95+ level enchantments), modded anvils (e.g., *Anvil Lawful*), and direct level modification mods (e.g., *LevelUp2*).
- **Server & Singleplayer Friendly**: Authoritative on dedicated servers so vanilla clients can connect without issues. When installed on both client and server, level updates are 100% smooth.

---

## 💡 How It Works

### Vanilla vs. Fair Experience Example

Suppose you are **Level 46** and do a **Level 30** enchantment (which costs 3 levels):

| Game Behavior | Resulting Level | Raw XP Lost |
| :--- | :--- | :--- |
| **Vanilla Minecraft** | **Level 43** | `~910 XP` |
| **Fair Experience** | **Level 44 (+ 75%)** | `306 XP` *(exact cost of 30 $\rightarrow$ 27)* |

---

## ⚙️ Configuration

Fair Experience provides a simple configuration file located at `config/fairexperience.cfg`:

```hocon
# Configuration for Fair Experience

general {
    # If true, anvil level costs will be converted to fair raw XP costs.
    B:enableFairAnvils=true

    # If true, direct experienceLevel subtractions (e.g. by LevelUp2) will be converted to fair raw XP costs.
    B:enableFairDirectModifications=true

    # If true, enchanting table level costs will be converted to fair raw XP costs.
    B:enableFairEnchanting=true
}
```

---

## 📄 License

This mod is released under the **MIT License**. Feel free to include it in any modpack!
