import React from "react";
import { makeStyles } from "@material-ui/core/styles";
import Paper from "@material-ui/core/Paper";
import Typography from "@material-ui/core/Typography";
import WarningIcon from "@material-ui/icons/Warning";
import AccessAlarmsIcon from "@material-ui/icons/AccessAlarms";

const useStyles = makeStyles((theme) => ({
  root: {
    margin: "15px",
    position: "absolute",
    zIndex: "1",
    display: "flex",
    flexWrap: "wrap",
    "& > *": {
      margin: "0 15px 15px 0",
      width: theme.spacing(16),
      height: theme.spacing(6),
      padding: "10px",
    },
  },
  secondaryTitle: {
    fontSize: 12,
  },
  title: {
    fontSize: 16,
    fontWeight: "bold",
  },
}));

function getTruckConsumptionStatus(datapoint) {
  let consumption_ratio =
    datapoint.normal_consumption / datapoint.truck_consumption;
  let tolerance = 0.05;
  if (1 < consumption_ratio + tolerance && 1 <= consumption_ratio) {
    return 0;
  } else if (1 <= consumption_ratio + tolerance && 1 > consumption_ratio) {
    return 1;
  } else {
    return 2;
  }
}

//Function for counting the conspicuous warnings of the datapoint
function conspicuousCounter(datapoint) {
  let conspicuousCount = 0;
  if (datapoint.driver_acceleration === 1) {
    conspicuousCount++;
  }
  if (datapoint.driver_speed === 1) {
    conspicuousCount++;
  }
  if (datapoint.driver_brake === 1) {
    conspicuousCount++;
  }
  if (getTruckConsumptionStatus(datapoint) === 1) {
    conspicuousCount++;
  }
  if (datapoint.engine_efficiency === 1) {
    conspicuousCount++;
  }
  if (datapoint.tires_efficiency === 1) {
    conspicuousCount++;
  }
  if (datapoint.truck_condition === 1) {
    conspicuousCount++;
  }
  return conspicuousCount;
}

//Function for counting the bad warnings of the datapoint
function badCounter(datapoint) {
  let badCount = 0;
  if (datapoint.driver_acceleration === 2) {
    badCount++;
  }
  if (datapoint.driver_speed === 2) {
    badCount++;
  }
  if (datapoint.driver_brake === 2) {
    badCount++;
  }
  if (getTruckConsumptionStatus(datapoint) === 2) {
    badCount++;
  }
  if (datapoint.engine_efficiency === 2) {
    badCount++;
  }
  if (datapoint.tires_efficiency === 2) {
    badCount++;
  }
  if (datapoint.truck_condition === 2) {
    badCount++;
  }
  return badCount;
}
function warningCounter(datasample) {
  let totalWarnings = 0;
  datasample.map(
    (datapoint) =>
      (totalWarnings += conspicuousCounter(datapoint) + badCounter(datapoint))
  );

  return totalWarnings;
}

function warningInTrucks(datasample) {
  let warningInTrucks = datasample.filter(
    (datapoint) =>
      (conspicuousCounter(datapoint) > 0 ? 1 : 0) +
        (badCounter(datapoint) > 0 ? 1 : 0) >
      0
  );
  return warningInTrucks.length;
}

function delayCounter(datasample) {
  let totalDelays = datasample.filter((datapoint) => datapoint.delay > 0);
  return totalDelays.length;
}

export default function SimplePaper(props) {
  const classes = useStyles();

  return (
    <div className={classes.root}>
      <Paper elevation={1}>
        <Typography className={classes.title} color="Primary" gutterBottom>
          <WarningIcon style={{ fontSize: 15, marginRight: 5 }}></WarningIcon>
          {warningCounter(props.datasample)} Warnings
        </Typography>
        <Typography
          className={classes.secondaryTitle}
          color="textSecondary"
          gutterBottom
        >
          in{" "}
          <span style={{ color: "#3f51b5", fontWeight: "bold" }}>
            {warningInTrucks(props.datasample)}
          </span>{" "}
          Trucks
        </Typography>
      </Paper>
      <Paper elevation={1}>
        <Typography className={classes.title} color="Primary" gutterBottom>
          <AccessAlarmsIcon
            style={{ fontSize: 15, marginRight: 5 }}
          ></AccessAlarmsIcon>
          {delayCounter(props.datasample)} Trucks
        </Typography>
        <Typography
          className={classes.secondaryTitle}
          color="textSecondary"
          gutterBottom
        >
          have a delay
        </Typography>
      </Paper>
    </div>
  );
}
