import React from "react";
import { makeStyles } from "@material-ui/core/styles";
import Paper from "@material-ui/core/Paper";
import Grid from "@material-ui/core/Grid";

import Typography from "@material-ui/core/Typography";
import "../../../node_modules/react-vis/dist/style.css";
import Popover from "@material-ui/core/Popover";

import PanoramaWideAngleIcon from "@material-ui/icons/PanoramaWideAngle";

import SpeedIcon from "@material-ui/icons/Speed";
import LocalGasStationIcon from "@material-ui/icons/LocalGasStation";
import ReportProblemIcon from "@material-ui/icons/ReportProblem";
import HelpOutlineIcon from "@material-ui/icons/HelpOutline";
import Drivinghelpertext from "./Drivinginfohelpertext";

const useStyles = makeStyles((theme) => ({
  root: { marginBottom: theme.spacing(1) },

  paper: {
    padding: theme.spacing(2),
    color: theme.palette.text.secondary,
    boxShadow: "none",
  },
  header: {
    backgroundColor: "#3f51b5",
    padding: theme.spacing(2),
    color: "white",
  },
  behaviorDetail: {
    marginTop: theme.spacing(2),
    textAlign: "left",
  },
  behaviorIcon: {
    marginTop: theme.spacing(2),
    textAlign: "center",
  },
  typography: {
    padding: theme.spacing(2),
  },
}));

export default function Drivinginformation(props) {
  const classes = useStyles();

  // POPOVER INFO TEXT
  const [anchorEl, setAnchorEl] = React.useState(null);

  const handleClick = (event) => {
    setAnchorEl(event.currentTarget);
  };

  const handleClose = () => {
    setAnchorEl(null);
  };

  const open = Boolean(anchorEl);
  const id = open ? "simple-popover" : undefined;

  // End of Popover

  if (props.datapoint === undefined) {
    return (
      <div className="flex-container">
        <Paper className={classes.paper}>
          Please select a truck to get more details.
        </Paper>
      </div>
    );
  }

  const truck_speed = (props.datapoint.truck_speed * 3.6).toFixed(1);
  const avg_speed = (props.datapoint.avg_truck_speed * 3.6).toFixed(1);
  const truck_acceleration = props.datapoint.truck_acceleration;
  const avg_acceleration = props.datapoint.avg_truck_acceleration;
  const truck_consumption = props.datapoint.truck_consumption;
  const avg_consumption = props.datapoint.normal_consumption;
  // const truck_mass_tons = (props.datapoint.truck_mass / 1000).toFixed(1);

  return (
    <div className={classes.root}>
      <Grid container spacing={3}>
        <Grid item xs={12}>
          <Paper className={classes.paper}>
            <Grid container xs={12}>
              <Grid item xs={12}>
                <Typography variant="h6">
                  Driving Information{" "}
                  <HelpOutlineIcon
                    className="HelperIcon"
                    aria-describedby={id}
                    style={{ fontSize: "16px" }}
                    onClick={handleClick}
                  ></HelpOutlineIcon>
                  <Popover
                    id={id}
                    open={open}
                    anchorEl={anchorEl}
                    onClose={handleClose}
                    anchorOrigin={{
                      vertical: "bottom",
                      horizontal: "center",
                    }}
                    transformOrigin={{
                      vertical: "top",
                      horizontal: "center",
                    }}
                  >
                    <Typography className={classes.typography}>
                      <Drivinghelpertext></Drivinghelpertext>
                    </Typography>
                  </Popover>
                </Typography>
              </Grid>
              <Paper className={classes.paper}>
                <Grid container justify="center" xs={12}>
                  <Grid item xs={3} className={classes.behaviorIcon}>
                    <SpeedIcon></SpeedIcon>
                    <Typography style={{ fontSize: "10px" }}>Speed</Typography>
                  </Grid>
                  <Grid item xs={9} className={classes.behaviorDetail}>
                    <span style={{ fontWeight: "600", fontSize: "18px" }}>
                      {truck_speed}
                    </span>{" "}
                    / {avg_speed}{" "}
                    <span style={{ fontSize: "12px" }}> km/h</span>{" "}
                    {truck_speed > avg_speed ? (
                      <span>
                        <ReportProblemIcon
                          style={{
                            fontSize: "16px",
                            color: "rgb(63, 81, 181)",
                          }}
                        />
                      </span>
                    ) : (
                      ""
                    )}
                  </Grid>
                  <Grid item xs={3} className={classes.behaviorIcon}>
                    <PanoramaWideAngleIcon></PanoramaWideAngleIcon>
                    <Typography style={{ fontSize: "10px" }}>
                      Acceleration
                    </Typography>
                  </Grid>
                  <Grid item xs={9} className={classes.behaviorDetail}>
                    <span style={{ fontWeight: "600", fontSize: "18px" }}>
                      {truck_acceleration}
                    </span>{" "}
                    / {avg_acceleration}
                    <span style={{ fontSize: "12px" }}> m/s²</span>{" "}
                    {truck_acceleration > avg_acceleration ? (
                      <span>
                        <ReportProblemIcon
                          style={{
                            fontSize: "16px",
                            color: "rgb(63, 81, 181)",
                          }}
                        />
                      </span>
                    ) : (
                      ""
                    )}
                  </Grid>
                  <Grid item xs={3} className={classes.behaviorIcon}>
                    <LocalGasStationIcon></LocalGasStationIcon>
                    <Typography style={{ fontSize: "10px" }}>
                      Consumption
                    </Typography>
                  </Grid>
                  <Grid item xs={9} className={classes.behaviorDetail}>
                    <span style={{ fontWeight: "600", fontSize: "18px" }}>
                      {truck_consumption}
                    </span>{" "}
                    / {avg_consumption}
                    <span style={{ fontSize: "12px" }}> l/100km</span>{" "}
                    {truck_consumption > avg_consumption ? (
                      <span>
                        <ReportProblemIcon
                          style={{
                            fontSize: "16px",
                            color: "rgb(63, 81, 181)",
                          }}
                        />
                      </span>
                    ) : (
                      ""
                    )}
                  </Grid>
                </Grid>
              </Paper>
            </Grid>
          </Paper>
        </Grid>
      </Grid>
    </div>
  );
}
