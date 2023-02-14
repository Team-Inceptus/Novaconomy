# <img src="https://cdn.discordapp.com/attachments/894254760075603980/984954715555123281/novaconomy.png" style="height: 10%; width: 10%;"> Novaconomy
> Customizable Economies with own symbols, names, icons and more.

## 💸 **Break Free from Single-Currencies.**
Novaconomy gives you the freedom to create **multiple** economies of your own choice, just like in real life!
With the freedom to have your own symbols, icons, conversion rates, and even interest, you'll have all you need to have a proper Economy!

<h2 style="text-decoration: underline;">📓Changelog</h2>

### 👔 1.7.0 Update: Cunning Corporations
- New Features
  - **Heavy Optimization**
    - Economies, Businesses, and Corporations now have their own Caching system for faster reading times
      - This should improve response times to Natural Increases and loading GUIs like /balance
    - Optimize Business & Economy Serialization
  - Created Corporations
    - Corporations are parent organizations that give Businesses that join them benefits, such as profit increases
    - Corporations have an experience and leveling system, currently the only natural way of gaining experience is through profit and Corporation Achievements
    - You can own both a Business and a Corporation; Corporation Owners will automatically have their Business added to their Corporation
    - Other Corporation Features
  - Other Improvements 

**Markets will be moved to v1.7.1 because of time constraints**
### 📊 1.6.0 Update: Supreme Stonks
- New Features:
  - **Heavy Optimization**
    - Economies and Businesses are now stored in individual files/folders to speed up loading times
      - The plugin contains an automatic migration script that will migrate existing economies/plugins into the new format, and API that uses it is now deprecated.
      - This script is planned to be removed by Early 2023.
    - Players no longer contain a proper configuration check for values and instead use defaults
  - Created Business Advertising
    - Business Advertising Balance deposit, can use any economy and is more/less valuable based on the economies' conversion rates
    - Businesses can now advertise other businesses, toggleable in settings
    - Economies now have a "clickable" tag that means it can be used to remove in the advertising balance, and as payment to other businesses for advertising your business
    - "Automatic Deposit" Business Setting to automatically deposit 15% of product sales into Advertising  
  - Created Business Keywords
    - Businesses can specify up to 10 topics that relate to the business
    - You can search using these keywords in Business Discover
  - Created Player Statistics
    - Includes Transaction History from last 10 purchases 
- Bug Fixes:
  - #17 
  - #18
  - #19
- Other Improvements:
  - Rename Economy & Change Natural Increase, Icon, Custom Model Data ID, or Conversion Scale Commands
  - Rename Business & Change Business Icon Commands
  - Setting Descriptions
  - Korean Translation

### 👔 1.5.0 Update: Outstanding Organizations
- New Features:
  - Business Rating System, allowing others to rate your business.
  - Business Statistics, allowing you to track how your business is doing.
  - Personal and Business Settings, for optimizing your own plugin experience.
    - You can now specify if you personally want to receive notifications.
    - Configuration Notifications will serve as a default value and will not override any personal settings.
  - Added Finnish, Swedish, and Norwegian Bokmål translations.
- Bug Fixes:
  - Messages Cleanup & Color Fixes
  - Fixed Blocks being taken out of inventory when clicking too fast
  - Other Code Cleanup
- Other Improvements:
  - Business Discovery, for searching random businesses to increase advertisement.
  - Edit Pricing for Products
  - Deposit to Bank Option on Custom Tax Events

### 💰 1.4.0 Update: "It's High Noon"
- New Features:
  - Created custom Tax Events, callable from "/taxevent" command (requires `novaconomy.admin.tax_event`)
  - Created Balance Leaderboards
  - Created Bounties on Players
  - Created a user /createcheck command for withdrawing money into item form
    - /economy createcheck is an **admin command** that does not withdraw money from the player 
- Bug Fixes:
  - Messages are now stored in the JAR and can be deleted in the plugin folder
  - Fixed Inventory Interaction where Items can be taken out
  - Fixed Case-Insensitive Modifier Names and ones with Multiple Economies
  - Optimization & Performance Improvements, Code Cleanup
- Other Improvements:
  - Indirect Kills for KillIncrease
  - Enchantment Bonuses for Natural Causes
  - Online Taxes Option for Automatic Taxes/Custom Events (Only apply taxes if players are online)
  - Bulk Product Purchase in Businesses
  - Exclude Entities, Death Causes, Blocks, or Materials in Natural Causes

### 🏦 1.3.0 Update: Banks, Bugs n' Betters
- Created Banks and Taxes, a global currency holder that players can deposit and withdraw from (configurable for min/maxxes), as well as mandatory or optional taxes.
- Fixed MANY BUGS, such as non-ID meta-having items (i.e. enchanted items) getting stuck in inventories, interest being available to users (yikes), and so many more.
- Added Modifiers for Natural Causes, allowing you to override certain death causes, materials broken, and entities killed with your own.
- Economies can no longer have the same symbol (you might get a "This Economy already exists" error if you try to create a new one with the same symbol as an existing one).
- Optimized, Bug Fixed, and ready to go!

### 📈 1.2.0 Update: Business is Boomin
Capitalism at its finest: Create, sell, and capitalize on your own merchandise! Create up to 28 products with
unlimited stock and set the prices to whatever you want!

-----
## 🔮 Future Features
- [x] Treasury API Hook
- [x] Bounties
- [x] Custom Tax Events
- [x] Player/Business Settings
- [x] Business Advertising
- [ ] Business Investing
- [ ] Novaconomy Stock Market

-----
## 💻 Novaconomy API
[![](https://jitpack.io/v/Team-Inceptus/Novaconomy.svg)](https://jitpack.io/#Team-Inceptus/Novaconomy)
[![](https://jitci.com/gh/Team-Inceptus/Novaconomy/svg)](https://jitci.com/gh/Team-Inceptus/Novaconomy)
![GitHub release (latest by date)](https://img.shields.io/github/v/release/Team-Inceptus/Novaconomy)
![GitHub last commit](https://img.shields.io/github/last-commit/Team-Inceptus/Novaconomy)

Published as a JitPack repository
<details>
    <summary>Maven</summary>

```xml
	<project>
	    <repositories>
		    <repository>
		        <id>jitpack.io</id>
		        <url>https://jitpack.io</url>
		    </repository>
	    </repositories>
    
        <dependencies>
            <!-- Include just the API -->
            <dependency>
                <groupId>us.teaminceptus.Novaconomy</groupId>
                <artifactId>novaconomy-api</artifactId>
                <version>1.2.0</version>
            </dependency>
            <!-- Include the Actual Plugin -->
            <dependency>
                <groupId>us.teaminceptus.Novaconomy</groupId>
                <artifactId>novaconomy</artifactId>
                <version>1.2.0</version>
            </dependency>
        </dependencies>
	</project>
```
</details>

<details>
    <summary>Gradle</summary>

```gradle
		repositories {
			maven { url 'https://jitpack.io' }
		}

	 	dependencies {
	   	     implementation 'com.github.Novaconomy:novaconomy-api:Tag'
	   	     // Include the Actual Plugin
	   	     implementation 'us.teaminceptus.Novaconomy:novaconomy:1.2.0'
		}	
```
</details>

**JavaDocs**: [https://novaconomy.teaminceptus.us](https://novaconomy.teaminceptus.us)

-----
## 📷 Screenshots
![Screenshot1](https://cdn.discordapp.com/attachments/894254760075603980/1018685509641449532/2022-09-11_19.45.01.png)

![Screenshot2](https://media.discordapp.net/attachments/894254760075603980/1018685509410754600/2022-09-11_19.45.27.png)

![Screenshot3](https://media.discordapp.net/attachments/894254760075603980/1018685509054255194/2022-09-11_19.45.46.png)

![Screenshot4](https://media.discordapp.net/attachments/894254760075603980/1018685508781609011/2022-09-11_19.51.54.png)

![Screenshot5](https://media.discordapp.net/attachments/894254760075603980/1006817734664925295/2022-08-10_01.52.53.png)

![Screenshot6](https://media.discordapp.net/attachments/894254760075603980/1006817734337757224/2022-08-10_01.53.07.png)

![Screenshot7](https://media.discordapp.net/attachments/894254760075603980/1006817733972856902/2022-08-10_01.53.21.png)

![Screenshot8](https://media.discordapp.net/attachments/894254760075603980/1006817734983684096/2022-08-10_01.51.46.png)

![Screenshot9](https://cdn.discordapp.com/attachments/894254760075603980/998836906706083872/2022-07-19_01.19.59.png)

![Screenshot10](https://cdn.discordapp.com/attachments/894254760075603980/998836907876302938/2022-07-19_01.19.18.png)

![Screenshot11](https://cdn.discordapp.com/attachments/894254760075603980/987617972937957376/2022-06-18_02.20.03.png)

![Screenshot12](https://cdn.discordapp.com/attachments/894254760075603980/987617972480798740/2022-06-18_02.20.12.png)

![Screenshot13](https://cdn.discordapp.com/attachments/860730694551863328/949806777539653712/2022-03-05_17.12.13.png)

![Screenshot14](https://cdn.discordapp.com/attachments/860730694551863328/949806777917116476/2022-03-05_17.11.33.png)

![Screenshot15](https://cdn.discordapp.com/attachments/860730694551863328/949806778793721866/2022-03-05_17.09.03.png)

![Screenshot16](https://cdn.discordapp.com/attachments/860730694551863328/949806779343200326/2022-03-05_17.08.31.png)

![Screenshot17](https://cdn.discordapp.com/attachments/860730694551863328/949806779775205396/2022-03-05_17.08.18.png)

-----
