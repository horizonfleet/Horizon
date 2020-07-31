import React from "react";
import { makeStyles } from "@material-ui/core/styles";
import List from "@material-ui/core/List";
import ListItem from "@material-ui/core/ListItem";
import ListItemText from "@material-ui/core/ListItemText";
import FormGroup from "@material-ui/core/FormGroup";
import Grid from "@material-ui/core/Grid";

const useStyles = makeStyles((theme) => ({
  root: {
    flexGrow: 1,
    maxWidth: 752,
  },
  demo: {
    backgroundColor: theme.palette.background.paper,
  },
  title: {
    margin: theme.spacing(4, 0, 2),
  },
}));

export default function InteractiveList(props) {
  const classes = useStyles();

  return (
    <div className={classes.root}>
      <FormGroup row></FormGroup>
      <Grid container spacing={2}>
        <Grid item xs={12} md={6}>
          <div className={classes.demo}>
            <List>
              <ListItem>
                <ListItemText
                  primary={props.datapoint.number_plate}
                  secondary={(props.datapoint.delay / 60).toFixed(0)}
                />
              </ListItem>
            </List>
          </div>
        </Grid>
      </Grid>
    </div>
  );
}
