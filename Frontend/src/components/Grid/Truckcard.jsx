import React from "react";
import { makeStyles } from "@material-ui/core/styles";
import Grid from "@material-ui/core/Grid";
import Typography from "@material-ui/core/Typography";
import ErrorOutlineIcon from "@material-ui/icons/ErrorOutline";

const useStyles = makeStyles((theme) => ({
  root: {
    display: "flex",
    justifyContent: "center",
    flexWrap: "wrap",
    "& > *": {
      margin: theme.spacing(0.5),
    },
    paper: {
      padding: theme.spacing(1),
      textAlign: "center",
      color: theme.palette.text.secondary,
    },
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

function Truckcard(props) {
  const classes = useStyles();

  const truck_speed = (props.datapoint.truck_speed * 3.6).toFixed(1);
  const delay = (props.datapoint.delay / 60).toFixed(0);
  let selectedStyle = () =>
    props.selectedTruckId === props.datapoint.truck_id
      ? { boxShadow: "0 4px 8px 0 rgba(0, 0, 0, 0.15)" }
      : { boxShadow: "none" };
  return (
    <div
      className={classes.root}
      onClick={() => props.setSelectedTruckId(props.datapoint.truck_id)}
      style={selectedStyle()}
    >
      <Grid container spacing={2} alignItems="center">
        <Grid
          item
          xs={3}
          style={truck_speed > 0 ? { color: "#5BB5CE" } : { color: "#DD3838" }}
        >
          <Typography
            style={{ fontSize: "18px", fontWeight: "600", textAlign: "center" }}
          >
            {truck_speed}
          </Typography>
          <Typography style={{ fontSize: "10px", textAlign: "center" }}>
            KM/H
          </Typography>
        </Grid>
        <Grid item xs={9}>
          <Typography style={{ fontSize: "12px", fontWeight: "600" }}>
            {props.datapoint.number_plate}
          </Typography>
          <Typography
            style={{
              fontSize: "16px",
              fontWeight: "600",
              marginBottom: "10px",
              marginTop: "5px",
            }}
          >
            <div style={{ float: "left" }}>{props.datapoint.departure} </div>
            <div style={{ float: "left" }}>
              <span> </span> - <span> </span>
              {props.datapoint.arrival}
            </div>
          </Typography>
          <Grid container spacing={2} alignItems="center">
            <Grid item xs={4} md={4} sm={12}>
              <span style={{ fontSize: "10px", fontWeight: "500" }}>DELAY</span>
              <Typography
                style={
                  delay > 0
                    ? { fontSize: "14px", color: "#f44336" }
                    : { fontSize: "14px", color: "#ededed" }
                }
              >
                {delay > 0 ? "+" : ""}
                {delay} <span style={{ fontSize: "10px" }}>min</span>
              </Typography>
            </Grid>
            <Grid item xs={4} md={4} sm={12}>
              <span style={{ fontSize: "10px", fontWeight: "500" }}>
                CONSPICUOUS
              </span>
              <Typography
                style={
                  conspicuousCounter(props.datapoint) > 0
                    ? { fontSize: "14px", color: "#ff9800" }
                    : { fontSize: "14px", color: "#ededed" }
                }
              >
                <ErrorOutlineIcon
                  style={{ fontSize: "14px" }}
                ></ErrorOutlineIcon>{" "}
                {conspicuousCounter(props.datapoint)}
              </Typography>
            </Grid>
            <Grid item xs={4} md={4} sm={12}>
              <span style={{ fontSize: "10px", fontWeight: "500" }}>BAD</span>
              <Typography
                style={
                  badCounter(props.datapoint) > 0
                    ? { fontSize: "14px", color: "#f44336" }
                    : { fontSize: "14px", color: "#ededed" }
                }
              >
                <ErrorOutlineIcon
                  style={{ fontSize: "14px" }}
                ></ErrorOutlineIcon>{" "}
                {badCounter(props.datapoint)}
              </Typography>
            </Grid>
          </Grid>
        </Grid>
      </Grid>
    </div>
  );
}
export default Truckcard;
