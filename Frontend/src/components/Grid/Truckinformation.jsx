import React from "react";
import { makeStyles } from "@material-ui/core/styles";
import Paper from "@material-ui/core/Paper";
import Grid from "@material-ui/core/Grid";
import Typography from "@material-ui/core/Typography";
import "../../../node_modules/react-vis/dist/style.css";
import Popover from "@material-ui/core/Popover";
import HelpOutlineIcon from "@material-ui/icons/HelpOutline";
import TripOriginIcon from "@material-ui/icons/TripOrigin";
import Truckinfohelpertext from "./Truckinfohelpertext";
import SettingsIcon from "@material-ui/icons/Settings";
import BuildIcon from "@material-ui/icons/Build";

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
  informationIcon: {
    marginTop: theme.spacing(2),
    textAlign: "center",
  },
  truckDetail: {
    marginTop: theme.spacing(2),
    textAlign: "left",
  },
  typography: {
    padding: theme.spacing(2),
  },
}));

export default function Truckinformation(props) {
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

  if (props.datapoint === undefined) {
    return (
      <div className="flex-container">
        <Paper className={classes.paper}>
          Please select a truck to get more details.
        </Paper>
      </div>
    );
  }

  function renderTruckStatusText(param) {
    switch (param) {
      case 0:
        return "normal";
      case 1:
        return "conspicuous";
      case 2:
        return "bad";
      default:
        return "-";
    }
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

  function styleByStatus(param) {
    switch (param) {
      case 0:
        return { color: "#e3e3e3" };
      case 1:
        return { color: "#ff9800" };
      case 2:
        return { color: "#f44336" };
      default:
        return { color: "#2196f3" };
    }
  }

  return (
    <div className={classes.root}>
      <Grid container spacing={3} direction="column">
        <Grid item xs={12}>
          <Paper className={classes.paper}>
            <Grid container xs={12}>
              <Grid item xs={12}>
                <Typography variant="h6">
                  Truck Information{" "}
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
                      <Truckinfohelpertext></Truckinfohelpertext>
                    </Typography>
                  </Popover>
                </Typography>
              </Grid>

              <Grid
                item
                xs={4}
                className={classes.informationIcon}
                style={styleByStatus(props.datapoint.tires_efficiency)}
              >
                <TripOriginIcon></TripOriginIcon>

                <Typography style={{ fontSize: "14px", fontWeight: "500" }}>
                  {renderTruckStatusText(props.datapoint.tires_efficiency)}
                </Typography>
                <Typography style={{ fontSize: "10px" }}>TIRES</Typography>
              </Grid>
              <Grid
                item
                xs={4}
                className={classes.informationIcon}
                style={styleByStatus(props.datapoint.engine_efficiency)}
              >
                <SettingsIcon></SettingsIcon>
                <Typography style={{ fontSize: "14px", fontWeight: "500" }}>
                  {renderTruckStatusText(props.datapoint.engine_efficiency)}
                </Typography>
                <Typography style={{ fontSize: "10px" }}>ENGINE</Typography>
              </Grid>
              <Grid
                item
                xs={4}
                className={classes.informationIcon}
                style={styleByStatus(props.datapoint.truck_condition)}
              >
                <BuildIcon></BuildIcon>
                <Typography style={{ fontSize: "14px", fontWeight: "500" }}>
                  {renderTruckStatusText(props.datapoint.truck_condition)}
                </Typography>
                <Typography style={{ fontSize: "10px" }}>
                  TRUCK CONDITION
                </Typography>
              </Grid>
              <Grid item xs={3} className={classes.truckDetail}>
                <Typography style={{ fontSize: "12px" }}>
                  Truck Type:
                </Typography>
              </Grid>
              <Grid item xs={9} className={classes.truckDetail}>
                <Typography style={{ fontSize: "12px" }}>
                  {renderTruckTypeText(props.datapoint.truck_type)}
                </Typography>
              </Grid>
              <Grid item xs={3} className={classes.truckDetail}>
                <Typography style={{ fontSize: "12px" }}>Year:</Typography>
              </Grid>
              <Grid item xs={9} className={classes.truckDetail}>
                <Typography style={{ fontSize: "12px" }}>
                  {props.datapoint.year}
                </Typography>
              </Grid>
              <Grid item xs={3} className={classes.truckDetail}>
                <Typography style={{ fontSize: "12px" }}>Mass:</Typography>
              </Grid>
              <Grid item xs={9} className={classes.truckDetail}>
                <Typography style={{ fontSize: "12px" }}>
                  {(props.datapoint.truck_mass / 1000).toFixed(1)} t
                </Typography>
              </Grid>
              <Grid item xs={3} className={classes.truckDetail}>
                <Typography style={{ fontSize: "12px" }}>Service:</Typography>
              </Grid>
              <Grid item xs={9} className={classes.truckDetail}>
                <Typography style={{ fontSize: "12px" }}>
                  in {props.datapoint.next_service} km
                </Typography>
              </Grid>
            </Grid>
          </Paper>
        </Grid>
      </Grid>
    </div>
  );
}
