import React from "react";
import { makeStyles, withStyles } from "@material-ui/core/styles";
import Paper from "@material-ui/core/Paper";
import LinearProgress from "@material-ui/core/LinearProgress";

const useStyles = makeStyles((theme) => ({
  root: {
    flexGrow: 1,
  },
  paper: {
    padding: theme.spacing(2),
    color: theme.palette.text.secondary,
    boxShadow: "none",
  },
}));

const CustomLinearProgress = withStyles((theme) => ({
  root: {
    height: 10,
    borderRadius: 5,
  },
  colorPrimary: {
    backgroundColor:
      theme.palette.grey[theme.palette.type === "light" ? 200 : 700],
  },
  bar: {
    borderRadius: 5,
    backgroundColor: "#3f51b5",
  },
}))(LinearProgress);

function TruckDetailInformation(props) {
  const classes = useStyles();

  if (props.datapoint === undefined) {
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

  const truck_speed = (props.datapoint.truck_speed * 3.6).toFixed(1);
  const best_speed = (props.datapoint.best_speed * 3.6).toFixed(0);
  const truck_mass_tons = (props.datapoint.truck_mass / 1000).toFixed(1);
  const service_progress = (
    (1 - props.datapoint.next_service / props.datapoint.service_interval) *
    100
  ).toFixed(0);

  return (
    <div className="flex-container-column">
      <div className="truck-info-subcolumn">
        <div className="truck-info-title">Information</div>
      </div>

      <div className="truck-info-subcolumn">
        <div className="truck-info-subrow">
          <div className="truck-info-subtitle">Consumption</div>
          <div className="truck-info-data">
            {props.datapoint.truck_consumption} /{" "}
            {props.datapoint.best_consumption} Liter pro 100 km
          </div>
        </div>
        <div className="truck-info-progress">
          <CustomLinearProgress variant="determinate" value={80} />
        </div>
      </div>

      <div className="truck-info-subcolumn">
        <div className="truck-info-subtitle">Driving Behavior</div>
        <div className="truck-info-subrow">
          <div className="truck-info-text">Speed:</div>
          <div className="truck-info-data">
            {truck_speed} / {best_speed} m/s
          </div>
        </div>
        <div className="truck-info-subrow">
          <div className="truck-info-text">Acceleration:</div>
          <div className="truck-info-data">
            {props.datapoint.truck_acceleration} m/s<sup>2</sup>
          </div>
        </div>
        <div className="truck-info-subrow">
          <div className="truck-info-text">Eco Level:</div>
          <div className="truck-info-data">{props.datapoint.eco_level}%</div>
        </div>
      </div>

      <div className="truck-info-subcolumn">
        <div className="truck-info-subtitle">Truck Condition</div>
        <div className="truck-info-subrow">
          <div className="truck-info-text">Type:</div>
          <div className="truck-info-data">
            {renderTruckTypeText(props.datapoint.truck_type)}
          </div>
        </div>
        <div className="truck-info-subrow">
          <div className="truck-info-text">Year:</div>
          <div className="truck-info-data">{props.datapoint.year}</div>
          {console.log("datapoint Year: ", props.datapoint.year)}
        </div>
        <div className="truck-info-subrow">
          <div className="truck-info-text">Mass:</div>
          <div className="truck-info-data">{truck_mass_tons}t</div>
        </div>
        <div className="truck-info-subrow">
          <div className="truck-info-text">Engine Efficiency:</div>
          <div className="truck-info-data">
            {props.datapoint.engine_efficiency} / 3
          </div>
        </div>
        <div className="truck-info-subrow">
          <div className="truck-info-text">Tires Efficiency:</div>
          <div className="truck-info-data">
            {props.datapoint.tires_efficiency} / 3
          </div>
        </div>
      </div>

      <div className="truck-info-subcolumn">
        <div className="truck-info-subrow">
          <div className="truck-info-subtitle">Service</div>
          <div className="truck-info-data">
            in {props.datapoint.next_service} km
          </div>
        </div>
        <div className="truck-info-progress">
          <CustomLinearProgress
            variant="determinate"
            value={service_progress}
          />
        </div>
      </div>
    </div>
  );
}

export default TruckDetailInformation;
