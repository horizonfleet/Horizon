import React from "react";
import { makeStyles } from "@material-ui/core/styles";
import Typography from "@material-ui/core/Typography";
import Grid from "@material-ui/core/Grid";

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

export default function Drivinghelpertext() {
  const classes = useStyles();

  return (
    <Grid container spacing={1} className={classes.root}>
      <Grid item xs={12}>
        <Typography className={classes.pos} color="textSecondary">
          Displays information to the current
          <span style={{ fontWeight: "600" }}> Trip</span> such as speed,
          acceleration and consumption. The reference values are based on the
          average values of the route.
        </Typography>
      </Grid>

      <Grid item xs={12}></Grid>
    </Grid>
  );
}
