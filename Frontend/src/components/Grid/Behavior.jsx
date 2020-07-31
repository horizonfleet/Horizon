import React from "react";
import { makeStyles, withStyles } from "@material-ui/core/styles";
import Paper from "@material-ui/core/Paper";
import Grid from "@material-ui/core/Grid";
import Typography from "@material-ui/core/Typography";
import "../../../node_modules/react-vis/dist/style.css";
import Popover from "@material-ui/core/Popover";
import Helpertext from "./Behaviourhelpertext";
import TrendingFlatIcon from "@material-ui/icons/TrendingFlat";
import HelpOutlineIcon from "@material-ui/icons/HelpOutline";
import LinearProgress from "@material-ui/core/LinearProgress";

const useStyles = makeStyles((theme) => ({
  root: { marginBottom: theme.spacing(1) },

  paper: {
    padding: theme.spacing(2),
    color: theme.palette.text.secondary,
    boxShadow: "none",
    backgroundColor: "#ffffff",
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

const CustomLinearProgress = withStyles((theme) => ({
  root: {
    marginBottom: theme.spacing(2),
    height: 10,
    borderRadius: 5,
  },
  colorPrimary: {
    backgroundColor:
      theme.palette.grey[theme.palette.type === "light" ? 200 : 700],
  },
  bar: {
    borderRadius: 5,
    backgroundColor: "#1a90ff",
  },
}))(LinearProgress);

export default function Behavior(props) {
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

  function renderTruckStatusText(param) {
    switch (param) {
      case 0:
        return { fontWeight: "500", transform: "rotate(0deg)" };
      case 1:
        return {
          fontWeight: "500",
          transform: "rotate(45deg)",
          color: "#ff9800",
        };
      case 2:
        return {
          fontWeight: "500",
          transform: "rotate(90deg)",
          color: "#f44336",
        };
      default:
        return { fontWeight: "500", transform: "rotate(0deg)" };
    }
  }

  function styleByStatus(param) {
    switch (param) {
      case 0:
        return { fontSize: "10px" };
      case 1:
        return { fontSize: "10px", color: "#ff9800" };
      case 2:
        return { fontSize: "10px", color: "#f44336" };
      default:
        return { fontSize: "10px" };
    }
  }

  if (props.datapoint === undefined) {
    return (
      <div className="flex-container">
        <Paper className={classes.paper}>
          Please select a truck to get more details.
        </Paper>
      </div>
    );
  }

  function getTruckConsumptionStatus() {
    let consumption_ratio =
      props.datapoint.normal_consumption / props.datapoint.truck_consumption;
    let tolerance = 0.05;
    if (1 < consumption_ratio + tolerance && 1 <= consumption_ratio) {
      console.log("truck status ", 0);
      return 0;
    } else if (1 <= consumption_ratio + tolerance && 1 > consumption_ratio) {
      console.log("truck status ", 1);
      return 1;
    } else {
      console.log("truck status ", 2);
      return 2;
    }
  }

  const warn_factor =
    (6 -
      (props.datapoint.driver_acceleration +
        props.datapoint.driver_speed +
        props.datapoint.driver_brake)) /
    6;
  const consumption_factor =
    props.datapoint.normal_consumption /
    (props.datapoint.truck_consumption === 0
      ? 0.1
      : props.datapoint.truck_consumption);

  const eco_status = (warn_factor * consumption_factor * 100).toFixed(1);

  return (
    <div className={classes.root}>
      <Grid container spacing={3} direction="column">
        <Grid item xs={12}>
          <Paper className={classes.paper}>
            <Grid container xs={12}>
              <Grid item xs={12}>
                <Typography variant="h6">
                  Driving Behavior{" "}
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
                      <Helpertext></Helpertext>
                    </Typography>
                  </Popover>
                </Typography>
              </Grid>

              <Grid item xs={12}>
                <div className="truck-info-subcolumn">
                  <div className="truck-info-progress">
                    <div className="truck-info-subrow">
                      <div className="truck-info-subtitle">Ecostatus</div>
                      <div className="truck-info-data">{eco_status}%</div>
                    </div>
                    <CustomLinearProgress
                      variant="determinate"
                      value={eco_status > 100 ? 100 : eco_status}
                    />
                  </div>
                </div>
              </Grid>
              <Grid item xl={3} md={6} className={classes.behaviorIcon}>
                <TrendingFlatIcon
                  style={renderTruckStatusText(props.datapoint.driver_speed)}
                ></TrendingFlatIcon>
                <Typography style={styleByStatus(props.datapoint.driver_speed)}>
                  SPEED
                </Typography>
              </Grid>
              <Grid item xl={3} md={6} className={classes.behaviorIcon}>
                <TrendingFlatIcon
                  style={renderTruckStatusText(
                    props.datapoint.driver_acceleration
                  )}
                ></TrendingFlatIcon>
                <Typography
                  style={styleByStatus(props.datapoint.driver_acceleration)}
                >
                  ACCELERATION
                </Typography>
              </Grid>
              <Grid item xl={3} md={6} className={classes.behaviorIcon}>
                <TrendingFlatIcon
                  style={renderTruckStatusText(getTruckConsumptionStatus())}
                ></TrendingFlatIcon>
                <Typography style={styleByStatus(getTruckConsumptionStatus())}>
                  CONSUMPTION
                </Typography>
              </Grid>
              <Grid item xl={3} md={6} className={classes.behaviorIcon}>
                <TrendingFlatIcon
                  style={renderTruckStatusText(props.datapoint.driver_brake)}
                ></TrendingFlatIcon>
                <Typography style={styleByStatus(props.datapoint.driver_brake)}>
                  BRAKES
                </Typography>
              </Grid>
            </Grid>
          </Paper>
        </Grid>
      </Grid>
    </div>
  );
}
