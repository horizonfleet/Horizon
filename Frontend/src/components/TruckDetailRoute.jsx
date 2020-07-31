import React from "react";
import { makeStyles } from "@material-ui/core/styles";
import Paper from "@material-ui/core/Paper";
import Grid from "@material-ui/core/Grid";
import Typography from "@material-ui/core/Typography";
import Popover from "@material-ui/core/Popover";
import HelpOutlineIcon from "@material-ui/icons/HelpOutline";
import Routehelpertext from "./Grid/Routehelpertext";
import {
  FaClock,
  FaRoad,
  FaTree,
  FaCity,
  FaSun,
  FaCloudSun,
  FaWind,
  FaCloudRain,
  FaCloudShowersHeavy,
  FaPooStorm,
  FaSnowflake,
  FaMoon,
} from "react-icons/fa";

const useStyles = makeStyles((theme) => ({
  root: { marginBottom: theme.spacing(1) },
  paper: {
    padding: theme.spacing(2),
    color: theme.palette.text.secondary,
    boxShadow: "none",
    backgroundColor: "#ffffff",
  },
  typography: {
    padding: theme.spacing(2),
  },
}));

function TruckDetailRoute(props) {
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

  function renderWeatherIcon(param) {
    switch (param) {
      case 0:
        return <FaSun />;
      case 1:
        return <FaCloudSun />;
      case 2:
        return <FaWind />;
      case 3:
        return <FaCloudRain />;
      case 4:
        return <FaCloudShowersHeavy />;
      case 5:
        return <FaWind />;
      case 6:
        return <FaSnowflake />;
      case 7:
        return <FaSnowflake />;
      case 8:
        return <FaMoon />;
      default:
        return <FaPooStorm />;
    }
  }

  function renderWeatherText(param) {
    switch (param) {
      case 0:
        return "Sun";
      case 1:
        return "Clouds";
      case 2:
        return "Wind";
      case 3:
        return "Rain";
      case 4:
        return "Thunderstorm";
      case 5:
        return "Fog";
      case 6:
        return "Snow";
      case 7:
        return "Frost";
      case 8:
        return "Clear Night";
      default:
        return "Unknown Weather";
    }
  }

  function renderRoadIcon(param) {
    switch (param) {
      case "URBAN":
        return <FaCity />;
      case "INTERURBAN":
        return <FaTree />;
      case "HIGHWAY":
        return <FaRoad />;
      case "FREEWAY":
        return <FaRoad />;
      default:
        return <FaRoad />;
    }
  }

  function renderRoadText(param) {
    switch (param) {
      case "URBAN":
        return "Urban";
      case "INTERURBAN":
        return "Interurban";
      case "HIGHWAY":
        return "Highway";
      case "FREEWAY":
        return "Freeway";
      case "TRUCKARRIVED":
        return "No road, pausing";
      default:
        return "Unknown Road";
    }
  }
  function Unix_timestamp(t) {
    var dt = new Date(t * 1000);
    var hr = dt.getHours();
    var m = "0" + dt.getMinutes();
    return hr + ":" + m.substr(-2);
  }

  return (
    <div className={classes.root}>
      {/* // <div className="flex-container"> */}
      <Grid container spacing={3} alignItems="center">
        <Grid item xs={12}>
          <Paper className={classes.paper}>
            <Grid container spacing={1} alignItems="center">
              <Grid item xs={12}>
                <Typography variant="h6">
                  Route Information{" "}
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
                      <Routehelpertext />
                    </Typography>
                  </Popover>
                </Typography>
              </Grid>
              <Grid
                item
                xs={12}
                style={{
                  textAlign: "center",
                }}
              >
                <div className="detail-route-row">
                  <div className="left-detail-route-column">
                    <progress
                      id="progressbar"
                      value={props.datapoint.route_progress / 100}
                      max="1"
                    ></progress>
                  </div>
                  <div className="right-detail-route-column">
                    <div className="right-detail-column">
                      <Typography
                        style={{
                          fontSize: "12px",
                          fontWeight: "400",
                          textAlign: "left",
                          color: "#0000008A",
                        }}
                      >
                        Departure
                      </Typography>
                      <Typography
                        style={{
                          fontSize: "18px",
                          fontWeight: "400",
                          textAlign: "left",
                          color: "#0000008A",
                        }}
                      >
                        {props.datapoint.departure}
                      </Typography>
                      {/* <div className="truck-route-subtitle">Departure</div> */}
                      {/* <div className="truck-route-title">
                        {props.datapoint.departure}
                      </div> */}
                      <div className="detail-route-row">
                        <div className="truck-route-subtitle">
                          <FaClock
                            style={{
                              color: "#0000008A",
                            }}
                          />
                        </div>
                        <div className="truck-route-subtitle">
                          {Unix_timestamp(props.datapoint.departure_time)}
                        </div>
                      </div>
                    </div>
                    <div className="right-detail-column">
                      <Typography
                        style={{
                          fontSize: "14px",
                          fontWeight: "700",
                          textAlign: "left",
                          color: "#3f51b5",
                        }}
                      >
                        Arrival
                      </Typography>
                      <Typography
                        style={{
                          fontSize: "20px",
                          fontWeight: "700",
                          textAlign: "left",
                        }}
                      >
                        {props.datapoint.arrival}
                      </Typography>
                      <div className="detail-route-row">
                        <div className="truck-route-subtitle">
                          <FaClock />
                        </div>
                        <div className="truck-route-subtitle">
                          {Unix_timestamp(props.datapoint.arrival_time)}
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </Grid>
              <Grid
                item
                xs={6}
                st
                le={{
                  textAlign: "center",
                }}
              >
                <span style={{ fontSize: "10px", fontWeight: "500" }}>
                  WEATHER
                </span>
                <Typography>
                  <span
                    style={{
                      fontSize: "14px",
                      fontWeight: "500",
                      color: "#3f51b5",
                    }}
                  >
                    {renderWeatherIcon(props.datapoint.weather)}{" "}
                    {renderWeatherText(props.datapoint.weather)}
                  </span>
                </Typography>
              </Grid>
              <Grid
                item
                xs={6}
                style={{
                  textAlign: "center",
                }}
              >
                <span style={{ fontSize: "10px", fontWeight: "500" }}>
                  ROAD TYPE
                </span>
                <Typography>
                  <span
                    style={{
                      fontSize: "14px",
                      fontWeight: "500",
                      color: "#3f51b5",
                    }}
                  >
                    {renderRoadIcon(props.datapoint.road_type)}{" "}
                    {renderRoadText(props.datapoint.road_type)}
                  </span>
                </Typography>
              </Grid>
            </Grid>
          </Paper>
        </Grid>
      </Grid>
      {/* <Paper className={classes.paper}>
        <Grid container spacing={3} alignItems="center">
          <Grid
            item
            xs={6}
            style={{
              textAlign: "center",
            }}
          >
            <div className="detail-route-row">
              <div className="left-detail-route-column">
                {renderWeatherIcon(props.datapoint.weather)}
              </div>
              <div className="right-detail-column">
                <div className="truck-route-text">
                  {renderWeatherText(props.datapoint.weather)}
                </div>
              </div>
            </div>
          </Grid>
          <Grid
            item
            xs={6}
            style={{
              textAlign: "center",
            }}
          >
            <div className="detail-route-row">
              <div className="left-detail-route-column">
                {renderRoadIcon(props.datapoint.road_type)}
              </div>
              <div className="right-detail-column">
                <div className="truck-route-text">
                  {renderRoadText(props.datapoint.road_type)}
                </div>
              </div>
            </div>
            {renderIncident(props.datapoint.incident)}
          </Grid>
          <Grid
            item
            xs={12}
            style={{
              textAlign: "center",
            }}
          >
            <div className="detail-route-row">
              <div className="left-detail-route-column">
                <progress
                  id="progressbar"
                  value={props.datapoint.route_progress}
                  max="1"
                ></progress>
              </div>
              <div className="right-detail-route-column">
                <div className="right-detail-column">
                  <div className="truck-route-subtitle">Departure</div>
                  <div className="truck-route-title">
                    {props.datapoint.departure}
                  </div>
                  <div className="detail-route-row">
                    <div className="truck-route-subtitle">
                      <FaClock />
                    </div>
                    <div className="truck-route-subtitle">
                      {Unix_timestamp(props.datapoint.departure_time)}
                    </div>
                  </div>
                </div>
                <div className="right-detail-column">
                  <div className="truck-route-subtitle">Arrival</div>
                  <div className="truck-route-title">
                    {props.datapoint.arrival}
                  </div>
                  <div className="detail-route-row">
                    <div className="truck-route-subtitle">
                      <FaClock />
                    </div>
                    <div className="truck-route-subtitle">
                      {Unix_timestamp(props.datapoint.arrival_time)}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </Grid>
        </Grid>
      </Paper> */}
    </div>
  );
}

export default TruckDetailRoute;
