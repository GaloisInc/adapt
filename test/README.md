
### Build Anomaly Detector

```
$ cd ../osu_iforest
$ make
```

### Run Anomaly Detection Module

```
$ ./start.sh
Writing features to file: features.csv
Found 64 idents
Extracting feature for: c8RLI0E7ATyVwfVt+p1t4AjvlwI7hfIQyERar6rSA8U=
Extracting feature for: apjeInJRzQ8jaky+CAQB5H+AyrnHnIooDDNbIZEe7CM=
Extracting feature for: Urupygd2nsUpV1JvE27w5l9nKbAsWcrArEQVJ3PyJJI=
Extracting feature for: PzPi9E1J8qFerg23aA9ULTAo5bTVF6vmRvzE7aULAFM=
Extracting feature for: YOmkyJ3/o2YeXDZ9YJG0pCvXi7w8OghA27HBnom5rEo=
Extracting feature for: tSGKUWdOexIE744uxq2d1+eIE90ZM/o80B6J4+0Rw4s=
Extracting feature for: BTM8zliW7ZQZUfU31v91bq51KSHGNa+bUCo+S4FKHto=
...
...
...
Extracting feature for: 9iEsH2vI/S00Ixd2B/W0+tXOIRdZ22LT5H4AsYdewnw=
Extracting feature for: LrcXgwv8TIveK7VCMS+BZlHS5Bh1uuhIFNYBgbDZ3Eo=
Extracting feature for: wVPJyCksys7pkyhc/HBDn7H2ArpeejNxGwrf1pwXxYI=
Extracting feature for: apjeInJRzQ8jaky+CAQB5H+AyrnHnIooDDNbIZEe7CM=
Extracting feature for: m9+2ccABww7+gL9k9R832/d+HaAvK75t3MFCvLg55Aw=
Extracting feature for: apjeInJRzQ8jaky+CAQB5H+AyrnHnIooDDNbIZEe7CM=
Writing Finished
# Trees     = 100
# Samples   = 100
Original Data Dimension: 64,4
Anomaly score attach finished

```

### Test Only Anomaly Detection Module (No interaction with other module)

`$ ./start.sh`
