# RSD-dashboard

A simple prototype dashboard and harvester for the RSD. 

To compile: 

```
./gradlew build
./gradlew copyRuntimeLibs
```

You can then run the harvester like this: 

```
java -cp "./lib/*" nl.esciencecenter.rsd.HarvestRSD 2> err
```

This will retrieve data on software and mentions from the RSD and determine various metrics. The results will be saved in two CSV files: 

- data/high_level_metrics.csv 
- data/aggregated_stats.csv  

To view the results, push these file to and access the dashboards here:

- [dashboard](https://www.dashbuilder.org/kitchensink/?import=https://raw.githubusercontent.com/jmaassen/RSD-dashboard/main/src/dashboard/dashboard.yaml)
- [detailed graphs](https://www.dashbuilder.org/kitchensink/?import=https://raw.githubusercontent.com/jmaassen/RSD-dashboard/main/src/dashboard/detailed.yaml) (under construction)



