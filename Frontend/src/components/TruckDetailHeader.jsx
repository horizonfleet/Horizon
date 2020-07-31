import React from "react";
import { makeStyles } from "@material-ui/core/styles";
import Paper from "@material-ui/core/Paper";
import { FaTruck } from "react-icons/fa";
import Grid from "@material-ui/core/Grid";
import Typography from "@material-ui/core/Typography";

const useStyles = makeStyles((theme) => ({
  root: { marginBottom: theme.spacing(1) },
  paper: {
    padding: theme.spacing(2),
    color: theme.palette.text.secondary,
    boxShadow: "none",
    backgroundColor: "#ffffff",
  },
}));

function TruckDetailHeader(props) {
  const classes = useStyles();
  const datapoint = props.datasample.find(
    (truck) => truck.truck_id === props.selectedTruckId
  );

  if (datapoint === undefined) {
    return (
      <div className="flex-container">
        <Paper className={classes.paper}>
          Please select a truck to get more details.
        </Paper>
      </div>
    );
  }

  function renderTruckTypeText(param) {
    switch (param) {
      case "LOCAL":
        return "Local Delivery Truck";
      case "LONG_DISTANCE":
        return "Long-distance Truck";
      case "LONG_DISTANCE_TRAILER":
        return "Long-distance Truck with Trailer";
      default:
        return "Unknown Truck Type";
    }
  }

  return (
    <div className={classes.root}>
      <Grid container spacing={3} alignItems="center">
        <Grid item xs={12}>
          <Paper className={classes.paper}>
            <Grid container spacing={3} alignItems="center">
              <Grid
                item
                xs={3}
                style={{
                  textAlign: "center",
                }}
              >
                <FaTruck
                  style={{
                    width: "32px",
                    height: "32px",
                    paddingLeft: "8px",
                  }}
                />
              </Grid>
              <Grid item xs={9}>
                <Grid container alignItems="center">
                  <Grid item xs={12}>
                    <Typography
                      style={{
                        fontSize: "18px",
                        fontWeight: "600",
                      }}
                    >
                      {datapoint.number_plate}
                    </Typography>
                  </Grid>
                  <Grid item xs={12}>
                    <Typography
                      style={{
                        fontSize: "12px",
                        fontWeight: "400",
                        marginTop: "5px",
                      }}
                    >
                      {renderTruckTypeText(datapoint.truck_type)}
                    </Typography>
                  </Grid>
                </Grid>
              </Grid>
            </Grid>
          </Paper>
        </Grid>
      </Grid>
    </div>
  );
}

export default TruckDetailHeader;
