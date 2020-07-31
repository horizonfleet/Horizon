import React from "react";
import { makeStyles } from "@material-ui/core/styles";
import Typography from "@material-ui/core/Typography";
import Grid from "@material-ui/core/Grid";
import TripOriginIcon from "@material-ui/icons/TripOrigin";

const useStyles = makeStyles({
  root: {
    minWidth: 275,
    maxWidth: 400,
  },
  bullet: {
    display: "inline-block",
    margin: "0 2px",
    transform: "scale(0.8)",
  },
  title: {
    fontSize: 14,
  },
  pos: {
    marginBottom: 12,
  },
});

export default function Truckinfohelpertext() {
  const classes = useStyles();

  return (
    <Grid container spacing={1} className={classes.root}>
      <Grid item xs={12}>
        <Typography className={classes.pos} color="textSecondary">
          <span style={{ fontWeight: "600" }}>Truckinformation</span> of the
          selected truck including tire efficiency, engine efficiency and
          general truck condition.
        </Typography>
      </Grid>
      <Grid item xs={2}>
        <TripOriginIcon
          style={{ fontWeight: "500", color: "#e3e3e3" }}
        ></TripOriginIcon>
      </Grid>
      <Grid
        item
        xs={10}
        style={{
          fontWeight: "500",
          color: "#e3e3e3",
        }}
      >
        Normal Behaviour
        <Typography variant="body2" component="p" color="textSecondary">
          Tires are ok.
          {/* <br />
          {'"a benevolent smile"'} */}
        </Typography>
      </Grid>
      <Grid item xs={2}>
        <TripOriginIcon
          style={{
            fontWeight: "500",
            color: "#ff9800",
          }}
        ></TripOriginIcon>
      </Grid>
      <Grid
        item
        xs={10}
        style={{
          fontWeight: "500",
          color: "#ff9800",
        }}
      >
        Conspicuous Behaviour
        <Typography variant="body2" component="p" color="textSecondary">
          The tires should be checked.
          {/* <br />
          {'"a benevolent smile"'} */}
        </Typography>
      </Grid>
      <Grid item xs={2}>
        <TripOriginIcon
          style={{
            fontWeight: "500",
            color: "#f44336",
          }}
        ></TripOriginIcon>
      </Grid>
      <Grid
        item
        xs={10}
        style={{
          fontWeight: "500",
          color: "#f44336",
        }}
      >
        Bad Behaviour
        <Typography variant="body2" component="p" color="textSecondary">
          The tyres need to be checked.
          {/* <br />
          {'"a benevolent smile"'} */}
        </Typography>
      </Grid>

      <Typography variant="h5" component="h2"></Typography>

      <Grid item xs={12}>
        <Typography variant="body2" component="p" color="textSecondary">
          Similarly, <span style={{ fontWeight: "600" }}> Service </span> and{" "}
          <span style={{ fontWeight: "600" }}> Truck Condition </span>.
        </Typography>
      </Grid>
    </Grid>
  );
}
