# Folderized Fabric Mods
[![forthebadge](https://forthebadge.com/images/badges/powered-by-black-magic.svg)](https://forthebadge.com)

This mod allows for loading other mods for a folder stucture in the mods folder.

There are several folder types. you can specify a folder with multiple mod versions seperated by commas (ie.
`1.16.2,1.16.3`) or major version matching (ie. `1.15.X`).

While I will keep this working, it is definitely not good practices java. Please be prepared for cancer when looking at the code; if it doesn't make you upset, I don't know what will.

## This mod is very modloader specific
make sure to update the fabric-loader on your older minecraft versions to match.

Mod versions will specify which versions of the fabric loader they will work with before their own version number.

For example, the main branch currently works on Fabric Loader `0.10.6` through `0.11.1`, so the mod's filename will be `modfolders-0.10.6-0.11.1-version.jar`
where version is the actual version number.

## auto update (1.1.0+)
In `1.1.0+` a config json was added at `.minecraft/config/folderizedfabricmods.json` which can be used to update mods in folders, do note that this is a new feature and may have bugs, I am open to suggestions of ways to improve this feature. The following are several examples of auto update configurations:

### jsmacros (beta)
Do note that this one requires a github api key in order to work properly, it will prompt for access when it needs updates and cache the key to this json.

this json targets the `.minecraft/mods/1.16.5/` folder.
```json
{
  "updaters": {
    "1.16.5": [
      {
        "modfilename": "jsmacros-1.16.5-.+.jar",
        "source": "GITHUB_ACTION",
        "hashType": "GIT",
        "url": "https://api.github.com/repos/wagyourtail/jsmacros/actions/runs?branch\u003dmaster\u0026status\u003dcompleted"
      }
    ]
  }
}
```

### jsmacros (stable)

this json targets the `.minecraft/mods/1.16.5/` folder.
```json
{
  "updaters": {
    "1.16.5": [
      {
        "modfilename": "jsmacros-1.16.5-.+.jar",
        "source": "GITHUB_ACTION",
        "hashType": "GIT",
        "url": "https://api.github.com/repos/wagyourtail/jsmacros/releases"
      }
    ]
  }
}
```

### emc
this json targets the `.minecraft/mods/1.16.5/` folder.
```json
{
  "updaters": {
    "1.16.5": [
      {
        "modfilename": "EMC-.+",
        "source": "MAVEN_LATEST",
        "hashType": "SHA1",
        "url": "https://gitlab.com/EMC-Framework/maven/raw/master/me/deftware/EMC-F-v2/latest-1.16.5/EMC-F-v2-latest-1.16.5.jar"
      }
    ]
  }
}
```
