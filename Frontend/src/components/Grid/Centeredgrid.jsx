import React from "react";
import { makeStyles } from "@material-ui/core/styles";
import Paper from "@material-ui/core/Paper";
import Grid from "@material-ui/core/Grid";
import Sortsection from "./Sortsection";
import Filtersection from "./Filtersection";
import TruckDetailHeader from "../TruckDetailHeader";
import TruckDetailRoute from "../TruckDetailRoute";
import Typography from "@material-ui/core/Typography";
import Mapresponsive from "./Mapresponsive";
import Warninfo from "./Warninfo";
import Drivinginformation from "./Drivinginformation";
import Behavior from "./Behavior";
import Truckinformation from "./Truckinformation";
import Truckcard from "./Truckcard";
import Logo from "../../logo_text.png";

//Some styling settings for some recurring components
const useStyles = makeStyles((theme) => ({
  root: {
    flexGrow: 1,
  },
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
  truckCard: {
    marginBottom: theme.spacing(2),
    padding: "0 !important",
  },
  truckCardPaper: {
    color: theme.palette.text.secondary,
    boxShadow: "none",
  },
  loader: {
    width: "50%",
  },
}));

//Functional Component. This component is the "sceletton" of the frontend application. The layout is developed with grid
export default function CenteredGrid(props) {
  const classes = useStyles();
  const [selectedTruckId, setSelectedTruckId] = React.useState(1);

  return (
    <div className={classes.root}>
      <Grid container spacing={3}>
        <Grid item xs={12}>
          <Paper className={classes.header}>
            {" "}
            <img
              src={Logo}
              alt="Logo"
              style={{
                height: "30px",
              }}
            />
          </Paper>
        </Grid>
      </Grid>

      <Grid container spacing={3}>
        <Grid item xs={3}>
          <Paper className={classes.paper}>
            <Grid container spacing={2} alignItems="baseline">
              <Grid item xs={3} style={{ margin: "1px 0px 1px 0px" }}>
                <div style={{ textAlign: "Left", fontWeight: "600" }}>
                  Trucks
                </div>
              </Grid>
              <Grid item xs={9} style={{ margin: "0px 0px 1px 0px" }}>
                <Typography
                  id="range-slider"
                  gutterBottom
                  style={{ textAlign: "Right" }}
                >
                  <span style={{ textAlign: "Left", fontWeight: "600" }}>
                    {props.datasample.length}
                  </span>
                  /{props.state.datasample.length} Trucks displayed
                </Typography>
              </Grid>
            </Grid>
          </Paper>
        </Grid>
        <Grid item xs={6}>
          <Paper className={classes.paper}>
            <Grid
              container
              direction="row"
              alignItems="baseline"
              justify="center"
            >
              <Grid item xs={4}>
                <Sortsection
                  style={{ textAlign: "right" }}
                  handleAlertSwitch={props.handleAlertSwitch}
                ></Sortsection>
              </Grid>
              <Grid item xs={8}>
                <Filtersection
                  state={props.state}
                  datasample={props.datasample}
                  handleDelayChange={props.handleDelayChange}
                  handleClear={props.handleClear}
                ></Filtersection>
              </Grid>
            </Grid>
          </Paper>
        </Grid>
        <Grid item xs={3}>
          <Paper className={classes.paper}>
            <Typography id="range-slider" gutterBottom>
              Truckdetails
            </Typography>
          </Paper>
        </Grid>
      </Grid>

      <Grid container spacing={3} direction="row">
        <Grid item xs={3} className="truck-list-grid">
          {props.datasample.map((datapoint) => (
            <Grid
              item
              xs={12}
              className={classes.truckCard}
              key={datapoint.truck_id}
            >
              <Paper className={classes.truckCardPaper + " truckcard"}>
                <Truckcard
                  datasample={props.datasample}
                  datapoint={datapoint}
                  selectedTruckId={selectedTruckId}
                  setSelectedTruckId={setSelectedTruckId}
                ></Truckcard>
              </Paper>
            </Grid>
          ))}
        </Grid>
        <Grid item xs={6}>
          <Paper className={classes.paper}>
            {/* <Delayalarm data={props.datasample}></Delayalarm> */}
            <Warninfo datasample={props.datasample}></Warninfo>
            <Mapresponsive
              datasample={props.datasample}
              selectedTruckId={selectedTruckId}
              setSelectedTruckId={setSelectedTruckId}
            ></Mapresponsive>
          </Paper>
        </Grid>
        <Grid item xs={3} className="truck-list-grid">
          <TruckDetailHeader
            key={selectedTruckId}
            selectedTruckId={selectedTruckId}
            datasample={props.datasample}
          />
          <TruckDetailRoute
            key={"detail-route"}
            datapoint={props.datasample.find(
              (truck) => truck.truck_id === selectedTruckId
            )}
          />
          <Drivinginformation
            key={"driving-information"}
            datapoint={props.datasample.find(
              (truck) => truck.truck_id === selectedTruckId
            )}
          ></Drivinginformation>
          <Behavior
            key={"behaviour-information"}
            datapoint={props.datasample.find(
              (truck) => truck.truck_id === selectedTruckId
            )}
          ></Behavior>
          <Truckinformation
            key={"truck-information"}
            datapoint={props.datasample.find(
              (truck) => truck.truck_id === selectedTruckId
            )}
          ></Truckinformation>

          {/* <TruckDetailInformation
            key={"detail-information"}
            datapoint={props.datasample.find(
              (truck) => truck.truck_id === selectedTruckId
            )}
          /> */}
        </Grid>
      </Grid>
    </div>
  );
}
